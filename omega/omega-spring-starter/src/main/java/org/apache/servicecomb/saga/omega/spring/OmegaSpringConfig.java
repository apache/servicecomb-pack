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

import com.google.common.collect.ImmutableList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.servicecomb.saga.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.saga.omega.connector.grpc.GrpcTccEventService;
import org.apache.servicecomb.saga.omega.connector.grpc.LoadBalancedClusterMessageSender;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.context.UniqueIdGenerator;
import org.apache.servicecomb.saga.omega.format.KryoMessageFormat;
import org.apache.servicecomb.saga.omega.format.MessageFormat;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.tcc.DefaultParametersContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.ParametersContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
class OmegaSpringConfig {

  @Bean(name = {"omegaUniqueIdGenerator"})
  IdGenerator<String> idGenerator() {
    return new UniqueIdGenerator();
  }

  @Bean
  OmegaContext omegaContext(@Qualifier("omegaUniqueIdGenerator") IdGenerator<String> idGenerator) {
    return new OmegaContext(idGenerator);
  }

  @Bean(name = {"compensationContext"})
  CallbackContext compensationContext(OmegaContext omegaContext) {
    return new CallbackContext(omegaContext);
  }

  @Bean(name = {"coordinateContext"})
  CallbackContext coordinateContext(OmegaContext omegaContext) {
    return new CallbackContext(omegaContext);
  }

  @Bean
  ServiceConfig serviceConfig(@Value("${spring.application.name}") String serviceName) {
    return new ServiceConfig(serviceName);
  }

  @Bean
  ParametersContext parametersContext() {
    return new DefaultParametersContext();
  }

  @Bean
  MessageSender grpcMessageSender(
      @Value("${alpha.cluster.address:localhost:8080}") String[] addresses,
      @Value("${alpha.cluster.ssl.enable:false}") boolean enableSSL,
      @Value("${alpha.cluster.ssl.mutualAuth:false}") boolean mutualAuth,
      @Value("${alpha.cluster.ssl.cert:client.crt}") String cert,
      @Value("${alpha.cluster.ssl.key:client.pem}") String key,
      @Value("${alpha.cluster.ssl.certChain:ca.crt}") String certChain,
      @Value("${omega.connection.reconnectDelay:3000}") int reconnectDelay,
      ServiceConfig serviceConfig,
      @Lazy MessageHandler handler) {

    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(ImmutableList.copyOf(addresses))
        .enableSSL(enableSSL)
        .enableMutualAuth(mutualAuth)
        .cert(cert)
        .key(key)
        .certChain(certChain)
        .messageDeserializer(messageFormat)
        .messageSerializer(messageFormat)
        .messageHandler(handler)
        .build();

    final MessageSender sender = new LoadBalancedClusterMessageSender(
        clusterConfig,
        serviceConfig,
        reconnectDelay);

    sender.onConnected();
    
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        sender.onDisconnected();
        sender.close();
      }
    }));
    return sender;
  }

  // TODO should integrate with loadBalance message sender in future.
  @Bean
  TccEventService tccEventService(ServiceConfig serviceConfig,
      @Lazy org.apache.servicecomb.saga.omega.transaction.tcc.MessageHandler coordinateMessageHandler,
      @Value("${alpha.cluster.address:localhost:8080}") String address) {
    ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
    final GrpcTccEventService service = new GrpcTccEventService(serviceConfig, channel, address, coordinateMessageHandler);
    // Need to register it self first
    service.onConnected();

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        service.onDisconnected();
        service.close();
      }
    }));
    return service;
  }
}
