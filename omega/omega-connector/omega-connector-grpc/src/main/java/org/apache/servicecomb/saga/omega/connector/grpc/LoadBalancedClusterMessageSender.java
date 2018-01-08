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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class LoadBalancedClusterMessageSender implements MessageSender {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<MessageSender, Long> senders = new HashMap<>();
  private final Collection<ManagedChannel> channels;

  public LoadBalancedClusterMessageSender(String[] addresses,
      MessageSerializer serializer,
      MessageDeserializer deserializer,
      ServiceConfig serviceConfig,
      MessageHandler handler) {

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
          new GrpcClientMessageSender(channel,
              serializer,
              deserializer,
              serviceConfig,
              handler),
          0L);
    }
  }

  LoadBalancedClusterMessageSender(MessageSender... messageSenders) {
    for (MessageSender sender : messageSenders) {
      senders.put(sender, 0L);
    }
    channels = emptyList();
  }

  @Override
  public void onConnected() {
    senders.keySet().forEach(MessageSender::onConnected);
  }

  @Override
  public void onDisconnected() {
    senders.keySet().forEach(MessageSender::onDisconnected);
  }

  @Override
  public void close() {
    channels.forEach(ManagedChannel::shutdownNow);
  }

  @Override
  public void send(TxEvent event) {
    boolean success = false;
    do {
      try {
        withFastestSender(messageSender -> {
          // very large latency on exception
          senders.put(messageSender, Long.MAX_VALUE);

          long startTime = System.nanoTime();
          messageSender.send(event);
          senders.put(messageSender, System.nanoTime() - startTime);
        });

        success = true;
      } catch (Exception e) {
        log.error("Retry sending event {} due to failure", event, e);
      }
    } while (!success && !Thread.currentThread().isInterrupted());
  }

  private void withFastestSender(Consumer<MessageSender> consumer) {
    senders.entrySet()
        .stream()
        .min(Comparator.comparingLong(Entry::getValue))
        .map(Entry::getKey)
        .ifPresent(consumer);
  }
}
