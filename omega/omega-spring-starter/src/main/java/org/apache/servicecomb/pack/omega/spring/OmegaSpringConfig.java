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
import org.apache.servicecomb.pack.omega.configuration.AlphaSSLProperties;
import org.apache.servicecomb.pack.omega.configuration.OmegaClientProperties;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextBuilder;
import org.apache.servicecomb.pack.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.pack.omega.connector.grpc.saga.SagaLoadBalanceSender;
import org.apache.servicecomb.pack.omega.connector.grpc.tcc.TccLoadBalanceSender;
import org.apache.servicecomb.pack.omega.context.CallbackContext;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.context.UniqueIdGenerator;
import org.apache.servicecomb.pack.omega.format.KryoMessageFormat;
import org.apache.servicecomb.pack.omega.format.MessageFormat;
import org.apache.servicecomb.pack.omega.spring.properties.BootAlphaClusterProperties;
import org.apache.servicecomb.pack.omega.spring.properties.BootOmegaClientProperties;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.DefaultParametersContext;
import org.apache.servicecomb.pack.omega.transaction.tcc.ParametersContext;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableConfigurationProperties({
    BootAlphaClusterProperties.class, BootOmegaClientProperties.class
})
class OmegaSpringConfig {

  private final BootAlphaClusterProperties alphaClusterProperties;

  private final BootOmegaClientProperties omegaClientProperties;

  public OmegaSpringConfig(
      BootAlphaClusterProperties alphaClusterProperties, BootOmegaClientProperties omegaClientProperties) {
    this.alphaClusterProperties = alphaClusterProperties;
    this.omegaClientProperties = omegaClientProperties;
  }

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
  ServiceConfig serviceConfig(@Value("${spring.application.name}") String serviceName, @Value("${omega.instance.instanceId:#{null}}") String instanceId) {
    return new ServiceConfig(serviceName,instanceId);
  }

  @Bean
  ParametersContext parametersContext() {
    return new DefaultParametersContext();
  }

  @Bean
  AlphaClusterConfig alphaClusterConfig(BootAlphaClusterProperties alphaClusterProperties,
      @Lazy MessageHandler handler,
      @Lazy TccMessageHandler tccMessageHandler) {

    AlphaSSLProperties ssl = alphaClusterProperties.getSsl();
    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(ImmutableList.copyOf(alphaClusterProperties.getAddress()))
        .enableSSL(ssl.isEnableSSL())
        .enableMutualAuth(ssl.isMutualAuth())
        .cert(ssl.getCert())
        .key(ssl.getKey())
        .certChain(ssl.getCertChain())
        .messageDeserializer(messageFormat)
        .messageSerializer(messageFormat)
        .messageHandler(handler)
        .tccMessageHandler(tccMessageHandler)
        .build();
    return clusterConfig;
  }

  @Bean(name = "sagaLoadContext")
  LoadBalanceContext sagaLoadBalanceSenderContext(
      BootOmegaClientProperties omegaClientProperties,
      AlphaClusterConfig alphaClusterConfig,
      ServiceConfig serviceConfig) {
    LoadBalanceContext loadBalanceSenderContext = new LoadBalanceContextBuilder(
        TransactionType.SAGA,
        alphaClusterConfig,
        serviceConfig,
        omegaClientProperties.getReconnectDelayMilliSeconds(),
        omegaClientProperties.getTimeoutSeconds()).build();
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
      OmegaClientProperties omegaClientProperties) {
    LoadBalanceContext loadBalanceSenderContext = new LoadBalanceContextBuilder(
        TransactionType.TCC,
        alphaClusterConfig,
        serviceConfig,
        omegaClientProperties.getReconnectDelayMilliSeconds(),
        omegaClientProperties.getTimeoutSeconds()).build();
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
