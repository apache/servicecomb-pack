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

import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.NameResolver.Factory;
import io.grpc.util.RoundRobinLoadBalancerFactory;

public class LoadBalancedClusterMessageSender implements MessageSender {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MessageSender messageSender;

  public LoadBalancedClusterMessageSender(String addresses,
      MessageSerializer serializer,
      MessageDeserializer deserializer,
      ServiceConfig serviceConfig,
      MessageHandler handler) {

    this(new GrpcClientMessageSender(clusterDirectAddressChannel(addresses),
        serializer,
        deserializer,
        serviceConfig,
        handler));
  }

  LoadBalancedClusterMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  private static ManagedChannel clusterDirectAddressChannel(String addresses) {
    return ManagedChannelBuilder.forTarget(addresses)
        .nameResolverFactory(new ClusterNameResolverFactory(addresses))
        .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
        .usePlaintext(true)
        .build();
  }

  @Override
  public void onConnected() {
    messageSender.onConnected();
  }

  @Override
  public void onDisconnected() {
    messageSender.onDisconnected();
  }

  @Override
  public void send(TxEvent event) {
    boolean success = false;
    do {
      try {
        messageSender.send(event);
        success = true;
      } catch (Exception e) {
        log.error("Retry sending event {} due to failure", event, e);
      }
    } while (!success && !Thread.currentThread().isInterrupted());

  }

  private static class ClusterNameResolverFactory extends Factory {
    private final String addresses;

    private ClusterNameResolverFactory(String addresses) {
      this.addresses = addresses;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
      return new NameResolver() {
        @Override
        public String getServiceAuthority() {
          return "localhost";
        }

        @Override
        public void start(final Listener listener) {
          List<SocketAddress> socketAddresses = Arrays.stream(addresses.split(","))
              .map(address -> {
                String[] split = address.split(":");
                return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
              })
              .collect(Collectors.toList());

          listener.onAddresses(
              Arrays.asList(new EquivalentAddressGroup(socketAddresses.get(0)),
                  new EquivalentAddressGroup(socketAddresses.get(1))),
              Attributes.EMPTY);
        }

        @Override
        public void shutdown() {
        }
      };
    }

    @Override
    public String getDefaultScheme() {
      return "directaddress";
    }
  }
}
