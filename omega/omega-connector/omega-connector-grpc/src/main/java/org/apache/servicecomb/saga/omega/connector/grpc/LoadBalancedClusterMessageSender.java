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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class LoadBalancedClusterMessageSender implements MessageSender {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<MessageSender, Long> senders = new ConcurrentHashMap<>();
  private final Collection<ManagedChannel> channels;

  private final BlockingQueue<Runnable> pendingTasks = new LinkedBlockingQueue<>();
  private final BlockingQueue<MessageSender> availableMessageSenders = new LinkedBlockingQueue<>();
  private final MessageSender retryableMessageSender = new RetryableMessageSender(availableMessageSenders);
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  public LoadBalancedClusterMessageSender(String[] addresses,
      MessageSerializer serializer,
      MessageDeserializer deserializer,
      ServiceConfig serviceConfig,
      MessageHandler handler,
      int reconnectDelay) {

    if (addresses.length == 0) {
      throw new IllegalArgumentException("No reachable cluster address provided");
    }

    channels = new ArrayList<>(addresses.length);
    for (String address : addresses) {
      ManagedChannel channel = ManagedChannelBuilder.forTarget(address)
          .usePlaintext(true)
          .build();

      channels.add(channel);
      senders.put(
          new GrpcClientMessageSender(
              address,
              channel,
              serializer,
              deserializer,
              serviceConfig,
              errorHandlerFactory(),
              handler),
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
    senders.keySet().forEach(sender -> {
      try {
        sender.onConnected();
      } catch (Exception e) {
        LOG.error("Failed connecting to alpha at {}", sender.target(), e);
      }
    });
  }

  @Override
  public void onDisconnected() {
    senders.keySet().forEach(sender -> {
      try {
        sender.onDisconnected();
      } catch (Exception e) {
        LOG.error("Failed disconnecting from alpha at {}", sender.target(), e);
      }
    });
  }

  @Override
  public void close() {
    scheduler.shutdown();
    channels.forEach(ManagedChannel::shutdownNow);
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    do {
      MessageSender messageSender = fastestSender();

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

  private MessageSender fastestSender() {
    return senders.entrySet()
        .stream()
        .filter(entry -> entry.getValue() < Long.MAX_VALUE)
        .min(Comparator.comparingLong(Entry::getValue))
        .map(Entry::getKey)
        .orElse(retryableMessageSender);
  }

  private void scheduleReconnectTask(int reconnectDelay) {
    scheduler.scheduleWithFixedDelay(() -> {
      try {
        pendingTasks.take().run();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, 0, reconnectDelay, MILLISECONDS);
  }

  private Function<MessageSender, Runnable> errorHandlerFactory() {
    return messageSender -> {
      Runnable runnable = new PushBackReconnectRunnable(messageSender, senders, pendingTasks, availableMessageSenders);
      return () -> pendingTasks.offer(runnable);
    };
  }
}
