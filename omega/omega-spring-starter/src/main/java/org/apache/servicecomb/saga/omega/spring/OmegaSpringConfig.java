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

package org.apache.servicecomb.saga.omega.spring;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.servicecomb.saga.omega.connector.grpc.GrpcClientMessageSender;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.context.UniqueIdGenerator;
import org.apache.servicecomb.saga.omega.format.NativeMessageFormat;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Configuration
class OmegaSpringConfig {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final List<ManagedChannel> channels = new ArrayList<>();
  private final List<MessageSender> senders = new ArrayList<>();

  @Bean
  IdGenerator<String> idGenerator() {
    return new UniqueIdGenerator();
  }

  @Bean
  OmegaContext omegaContext(IdGenerator<String> idGenerator) {
    return new OmegaContext(idGenerator);
  }

  @Bean
  ServiceConfig serviceConfig(@Value("${spring.application.name}") String serviceName) {
    return new ServiceConfig(serviceName);
  }

  @PreDestroy
  void close() {
    senders.forEach(MessageSender::onDisconnected);
    channels.forEach(ManagedChannel::shutdown);
  }

  @Bean
  MessageSender grpcMessageSender(@Value("${alpha.cluster.address}") String[] addresses, ServiceConfig serviceConfig,
      @Lazy MessageHandler handler) {
    // TODO: 2017/12/26 connect to the one with lowest latency
    for (String address : addresses) {
      try {
        MessageSender sender = new GrpcClientMessageSender(grpcChannel(address), new NativeMessageFormat(), new NativeMessageFormat(), serviceConfig, handler);
        sender.onConnected();
        senders.add(sender);
        return sender;
      } catch (Exception e) {
        log.error("Unable to connect to alpha at {}", address, e);
      }
    }

    throw new IllegalArgumentException(
        "None of the alpha cluster is reachable: " + Arrays.toString(addresses));
  }

  private ManagedChannel grpcChannel(String address) {
    String[] pair = address.split(":");

    ManagedChannel channel = ManagedChannelBuilder.forAddress(pair[0], Integer.parseInt(pair[1]))
        .usePlaintext(true)
        .build();

    channels.add(channel);
    return channel;
  }
}
