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

package org.apache.servicecomb.pack.omega.spring;

import com.google.common.collect.ImmutableList;
import org.apache.servicecomb.pack.common.AlphaMetaKeys;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterDiscovery;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextBuilder;
import org.apache.servicecomb.pack.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.pack.omega.connector.grpc.saga.SagaLoadBalanceSender;
import org.apache.servicecomb.pack.omega.connector.grpc.tcc.TccLoadBalanceSender;
import org.apache.servicecomb.pack.omega.transaction.CallbackContext;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.context.UniqueIdGenerator;
import org.apache.servicecomb.pack.omega.format.KryoMessageFormat;
import org.apache.servicecomb.pack.omega.format.MessageFormat;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.DefaultParametersContext;
import org.apache.servicecomb.pack.omega.transaction.tcc.ParametersContext;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.lang.invoke.MethodHandles;

@Configuration
class OmegaSpringConfig {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Bean(name = {"omegaUniqueIdGenerator"})
  IdGenerator<String> idGenerator() {
    return new UniqueIdGenerator();
  }

  @Bean
  OmegaContext omegaContext(@Qualifier("omegaUniqueIdGenerator") IdGenerator<String> idGenerator, SagaMessageSender messageSender) {
    ServerMeta serverMeta = messageSender.onGetServerMeta();
    boolean akkaEnabeld = Boolean.parseBoolean(serverMeta.getMetaMap().get(AlphaMetaKeys.AkkaEnabled.name()));
    return new OmegaContext(idGenerator,akkaEnabeld);
  }

  @Bean(name = {"compensationContext"})
  CallbackContext compensationContext(OmegaContext omegaContext, SagaMessageSender sender) {
    return new CallbackContext(omegaContext, sender);
  }

  @Bean(name = {"coordinateContext"})
  CallbackContext coordinateContext(OmegaContext omegaContext, SagaMessageSender sender) {
    return new CallbackContext(omegaContext, sender);
  }

  @Bean
  ServiceConfig serviceConfig(@Value("${spring.application.name}") String serviceName, @Value("${omega.instance.instanceId:#{null}}") String instanceId) {
    return new ServiceConfig(serviceName,instanceId);
  }

  @Bean
  ParametersContext parametersContext() {
    return new DefaultParametersContext();
  }

  @Bean
  @ConditionalOnProperty(name = "alpha.cluster.register.type", havingValue = "default", matchIfMissing = true)
  AlphaClusterDiscovery alphaClusterAddress(@Value("${alpha.cluster.address:0.0.0.0:8080}") String[] addresses){
    return AlphaClusterDiscovery.builder().addresses(addresses).build();
  }

  @Bean
  AlphaClusterConfig alphaClusterConfig(
      @Value("${alpha.cluster.ssl.enable:false}") boolean enableSSL,
      @Value("${alpha.cluster.ssl.mutualAuth:false}") boolean mutualAuth,
      @Value("${alpha.cluster.ssl.cert:client.crt}") String cert,
      @Value("${alpha.cluster.ssl.key:client.pem}") String key,
      @Value("${alpha.cluster.ssl.certChain:ca.crt}") String certChain,
      @Lazy AlphaClusterDiscovery alphaClusterDiscovery,
      @Lazy MessageHandler handler,
      @Lazy TccMessageHandler tccMessageHandler) {

    LOG.info("Discovery alpha cluster address {} from {}",alphaClusterDiscovery.getAddresses() == null ? "" : String.join(",",alphaClusterDiscovery.getAddresses()), alphaClusterDiscovery.getDiscoveryType().name());
    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(ImmutableList.copyOf(alphaClusterDiscovery.getAddresses()))
        .enableSSL(enableSSL)
        .enableMutualAuth(mutualAuth)
        .cert(cert)
        .key(key)
        .certChain(certChain)
        .messageDeserializer(messageFormat)
        .messageSerializer(messageFormat)
        .messageHandler(handler)
        .tccMessageHandler(tccMessageHandler)
        .build();
    return clusterConfig;
  }

  @Bean(name = "sagaLoadContext")
  LoadBalanceContext sagaLoadBalanceSenderContext(
      AlphaClusterConfig alphaClusterConfig,
      ServiceConfig serviceConfig,
      @Value("${omega.connection.reconnectDelay:3000}") int reconnectDelay,
      @Value("${omega.connection.sending.timeout:8}") int timeoutSeconds) {
    LoadBalanceContext loadBalanceSenderContext = new LoadBalanceContextBuilder(
        TransactionType.SAGA,
        alphaClusterConfig,
        serviceConfig,
        reconnectDelay,
        timeoutSeconds).build();
    return loadBalanceSenderContext;
  }

  @Bean
  SagaMessageSender sagaLoadBalanceSender(@Qualifier("sagaLoadContext") LoadBalanceContext loadBalanceSenderContext) {
    final SagaMessageSender sagaMessageSender = new SagaLoadBalanceSender(loadBalanceSenderContext, new FastestSender());
    sagaMessageSender.onConnected();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        sagaMessageSender.onDisconnected();
        sagaMessageSender.close();
      }
    }));
    return sagaMessageSender;
  }

  @Bean(name = "tccLoadContext")
  LoadBalanceContext loadBalanceSenderContext(
      AlphaClusterConfig alphaClusterConfig,
      ServiceConfig serviceConfig,
      @Value("${omega.connection.reconnectDelay:3000}") int reconnectDelay,
      @Value("${omega.connection.sending.timeout:8}") int timeoutSeconds) {
    LoadBalanceContext loadBalanceSenderContext = new LoadBalanceContextBuilder(
        TransactionType.TCC,
        alphaClusterConfig,
        serviceConfig,
        reconnectDelay,
        timeoutSeconds).build();
    return loadBalanceSenderContext;
  }

  @Bean
  TccMessageSender tccLoadBalanceSender(@Qualifier("tccLoadContext") LoadBalanceContext loadBalanceSenderContext) {
    final TccMessageSender tccMessageSender = new TccLoadBalanceSender(loadBalanceSenderContext, new FastestSender());
    tccMessageSender.onConnected();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        tccMessageSender.onDisconnected();
        tccMessageSender.close();
      }
    }));
    return tccMessageSender;
  }
}
