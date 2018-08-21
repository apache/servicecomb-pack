/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Supplier;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLException;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadBalancedClusterMessageSender implements MessageSender {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<MessageSender, Long> senders = new ConcurrentHashMap<>();
  private final Collection<ManagedChannel> channels;

  private final BlockingQueue<Runnable> pendingTasks = new LinkedBlockingQueue<>();
  private final BlockingQueue<MessageSender> availableMessageSenders = new LinkedBlockingQueue<>();
  private final MessageSender retryableMessageSender = new RetryableMessageSender(
      availableMessageSenders);

  private final Supplier<MessageSender> defaultMessageSender = new Supplier<MessageSender>() {
    @Override
    public MessageSender get() {
      return retryableMessageSender;
    }
  };

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  public LoadBalancedClusterMessageSender(AlphaClusterConfig clusterConfig,
      ServiceConfig serviceConfig,
      int reconnectDelay) {

    if (clusterConfig.getAddresses().isEmpty()) {
      throw new IllegalArgumentException("No reachable cluster address provided");
    }

    channels = new ArrayList<>(clusterConfig.getAddresses().size());

    SslContext sslContext = null;
    for (String address : clusterConfig.getAddresses()) {
      ManagedChannel channel;

      if (clusterConfig.isEnableSSL()) {
        if (sslContext == null) {
          try {
            sslContext = buildSslContext(clusterConfig);
          } catch (SSLException e) {
            throw new IllegalArgumentException("Unable to build SslContext", e);
          }
        }
         channel = NettyChannelBuilder.forTarget(address)
            .negotiationType(NegotiationType.TLS)
            .sslContext(sslContext)
            .build();
      } else {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext()
            .build();
      }
      channels.add(channel);
      senders.put(
          new GrpcClientMessageSender(
              address,
              channel,
              clusterConfig.getMessageSerializer(),
              clusterConfig.getMessageDeserializer(),
              serviceConfig,
              new ErrorHandlerFactory(),
              clusterConfig.getMessageHandler()),
          0L);
    }

    scheduleReconnectTask(reconnectDelay);
  }

  // this is for test only
  LoadBalancedClusterMessageSender(MessageSender... messageSenders) {
    for (MessageSender sender : messageSenders) {
      senders.put(sender, 0L);
    }
    channels = emptyList();
  }

  @Override
  public void onConnected() {
    for(MessageSender sender :senders.keySet()){
      try {
        sender.onConnected();
      } catch (Exception e) {
        LOG.error("Failed connecting to alpha at {}", sender.target(), e);
      }
    }
  }

  @Override
  public void onDisconnected() {
    for (MessageSender sender :senders.keySet()) {
      try {
        sender.onDisconnected();
      } catch (Exception e) {
        LOG.error("Failed disconnecting from alpha at {}", sender.target(), e);
      }
    }
  }

  @Override
  public void close() {
    scheduler.shutdown();
    for(ManagedChannel channel : channels) {
      channel.shutdownNow();
    }
  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    return send(event, new FastestSender());
  }

  AlphaResponse send(TxEvent event, MessageSenderPicker messageSenderPicker) {
    do {
      MessageSender messageSender = messageSenderPicker.pick(senders, defaultMessageSender);

      try {
        long startTime = System.nanoTime();
        AlphaResponse response = messageSender.send(event);
        senders.put(messageSender, System.nanoTime() - startTime);

        return response;
      } catch (OmegaException e) {
        throw e;
      } catch (Exception e) {
        LOG.error("Retry sending event {} due to failure", event, e);

        // very large latency on exception
        senders.put(messageSender, Long.MAX_VALUE);
      }
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to send event " + event + " due to interruption");
  }

  private void scheduleReconnectTask(int reconnectDelay) {
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          pendingTasks.take().run();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }, 0, reconnectDelay, MILLISECONDS);
  }

  class ErrorHandlerFactory {
    Runnable getHandler(MessageSender messageSender) {
      final Runnable runnable = new PushBackReconnectRunnable(messageSender, senders, pendingTasks,
          availableMessageSenders);
      return new Runnable() {
        @Override
        public void run() {
          pendingTasks.offer(runnable);
        }
      };
    }

  }

  private static SslContext buildSslContext(AlphaClusterConfig clusterConfig) throws SSLException {
    SslContextBuilder builder = GrpcSslContexts.forClient();
    // openssl must be used because some older JDk does not support cipher suites required by http2,
    // and the performance of JDK ssl is pretty low compared to openssl.
    builder.sslProvider(SslProvider.OPENSSL);

    Properties prop = new Properties();
    try {
      prop.load(LoadBalancedClusterMessageSender.class.getClassLoader().getResourceAsStream("ssl.properties"));
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read ssl.properties.", e);
    }

    builder.protocols(prop.getProperty("protocols").split(","));
    builder.ciphers(Arrays.asList(prop.getProperty("ciphers").split(",")));
    builder.trustManager(new File(clusterConfig.getCertChain()));

    if (clusterConfig.isEnableMutualAuth()) {
      builder.keyManager(new File(clusterConfig.getCert()), new File(clusterConfig.getKey()));
    }

    return builder.build();
  }
}

/**
 * The strategy of picking the fastest {@link MessageSender}
 */
class FastestSender implements MessageSenderPicker {

  @Override
  public MessageSender pick(Map<MessageSender, Long> messageSenders,
      Supplier<MessageSender> defaultSender) {
    Long min = Long.MAX_VALUE;
    MessageSender sender = null;
    for (Map.Entry<MessageSender, Long> entry : messageSenders.entrySet()) {
      if (entry.getValue() != Long.MAX_VALUE && min > entry.getValue()) {
        min = entry.getValue();
        sender = entry.getKey();
      }
    }
    if (sender == null) {
      return defaultSender.get();
    } else {
      return sender;
    }
  }
}