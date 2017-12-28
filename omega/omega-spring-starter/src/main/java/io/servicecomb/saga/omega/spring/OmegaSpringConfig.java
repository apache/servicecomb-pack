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

package io.servicecomb.saga.omega.spring;

import static com.google.common.net.HostAndPort.fromParts;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.swift.service.ThriftClientManager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.servicecomb.saga.omega.connector.grpc.GrpcClientMessageSender;
import io.servicecomb.saga.omega.connector.grpc.GrpcTxEventEndpointImpl;
import io.servicecomb.saga.omega.connector.thrift.ThriftMessageSender;
import io.servicecomb.saga.omega.context.IdGenerator;
import io.servicecomb.saga.omega.context.OmegaContext;
import io.servicecomb.saga.omega.context.UniqueIdGenerator;
import io.servicecomb.saga.omega.format.NativeMessageFormat;
import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.MessageSerializer;
import io.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc;
import io.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceBlockingStub;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEventEndpoint;

@Configuration
class OmegaSpringConfig {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ThriftClientManager clientManager = new ThriftClientManager();
  private final List<AutoCloseable> closeables = new ArrayList<>();

  private ManagedChannel clientChannel;

  @Bean
  IdGenerator<String> idGenerator() {
    return new UniqueIdGenerator();
  }

  @Bean
  OmegaContext omegaContext(IdGenerator<String> idGenerator) {
    return new OmegaContext(idGenerator);
  }

  //  @Bean
  MessageSender messageSender(@Value("${alpha.cluster.address}") String[] addresses) {
    // TODO: 2017/12/26 connect to the one with lowest latency
    for (String address : addresses) {
      try {
        String[] pair = address.split(":");
        ThriftMessageSender sender = createMessageSender(clientManager, pair[0], Integer.parseInt(pair[1]), new NativeMessageFormat());
        closeables.add(sender);
        return sender;
      } catch (Exception e) {
        log.error("Unable to connect to alpha at {}", address, e);
      }
    }

    throw new IllegalArgumentException(
        "None of the alpha cluster is reachable: " + Arrays.toString(addresses));
  }

  private ThriftMessageSender createMessageSender(ThriftClientManager clientManager,
      String host,
      int port,
      MessageSerializer serializer) {

    FramedClientConnector connector = new FramedClientConnector(fromParts(host, port));

    try {
      SwiftTxEventEndpoint endpoint = clientManager.createClient(connector, SwiftTxEventEndpoint.class).get();
      return new ThriftMessageSender(endpoint, serializer);
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Failed to create transaction event endpoint client to " + host + ":" + port, e);
    }
  }

  @PreDestroy
  void close() {
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception e) {
        log.warn("Failed to close message sender", e);
      }
    }

    clientManager.close();
    clientChannel.shutdown();
  }

  @Bean
  MessageSender grpcMessageSender(@Value("${alpha.cluster.address}") String[] addresses) {
    // TODO: 2017/12/26 connect to the one with lowest latency
    for (String address : addresses) {
      try {
        String[] pair = address.split(":");
        return createMessageSender(pair[0], Integer.parseInt(pair[1]), new NativeMessageFormat());
      } catch (Exception e) {
        log.error("Unable to connect to alpha at {}", address, e);
      }
    }

    throw new IllegalArgumentException(
        "None of the alpha cluster is reachable: " + Arrays.toString(addresses));
  }

  private GrpcClientMessageSender createMessageSender(String host, int port, MessageSerializer serializer) {
    clientChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
    TxEventServiceBlockingStub stub = TxEventServiceGrpc.newBlockingStub(clientChannel);
    GrpcTxEventEndpointImpl eventService = new GrpcTxEventEndpointImpl(stub);
    return new GrpcClientMessageSender(eventService, serializer);
  }
}
