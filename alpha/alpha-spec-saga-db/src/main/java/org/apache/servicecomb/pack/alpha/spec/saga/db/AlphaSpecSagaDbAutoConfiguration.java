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

package org.apache.servicecomb.pack.alpha.spec.saga.db;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.servicecomb.pack.alpha.core.CommandRepository;
import org.apache.servicecomb.pack.alpha.core.EventScanner;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.TxConsistentService;
import org.apache.servicecomb.pack.alpha.core.TxEventRepository;
import org.apache.servicecomb.pack.alpha.core.TxTimeoutRepository;
import org.apache.servicecomb.pack.alpha.core.api.APIv1;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.apache.servicecomb.pack.alpha.spec.saga.db.api.SagaDbAPIv1Controller;
import org.apache.servicecomb.pack.alpha.spec.saga.db.api.SagaDbAPIv1Impl;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.ClusterLockService;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.LockProviderJdbcConfiguration;
import org.apache.servicecomb.pack.alpha.spec.saga.db.metrics.AlphaMetricsEndpointImpl;
import org.apache.servicecomb.pack.alpha.spec.saga.db.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.saga.db.properties.SpecSagaDbProperties;
import org.apache.servicecomb.pack.alpha.spec.saga.db.test.AlphaEventController;
import org.apache.servicecomb.pack.common.AlphaMetaKeys;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.persistence.jpa.EclipseLinkJpaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ImportAutoConfiguration({SpecSagaDbProperties.class, EclipseLinkJpaConfiguration.class,
    LockProviderJdbcConfiguration.class})
@EnableJpaRepositories(basePackages = "org.apache.servicecomb.pack.alpha.spec.saga.db")
@ConditionalOnExpression("'${alpha.spec.names}'.contains('saga-db')")
public class AlphaSpecSagaDbAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public AlphaSpecSagaDbAutoConfiguration() {
    LOG.info("Alpha Specification Saga DB");
  }

  @Bean
  @Primary
  public DataSourceProperties dataSourceProperties(SpecSagaDbProperties specSagaDbProperties) {
    return specSagaDbProperties.getDatasource();
  }

  @Bean
  CommandRepository springCommandRepository(TxEventEnvelopeRepository eventRepo,
      CommandEntityRepository commandRepository) {
    return new SpringCommandRepository(eventRepo, commandRepository);
  }

  @Bean
  TxTimeoutRepository springTxTimeoutRepository(TxTimeoutEntityRepository timeoutRepo) {
    return new SpringTxTimeoutRepository(timeoutRepo);
  }

  @Bean
  TxEventRepository springTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    return new SpringTxEventRepository(eventRepo);
  }

  @Bean
  TxConsistentService txConsistentService(
      @Value("${alpha.event.pollingInterval:500}") int eventPollingInterval,
      @Value("${alpha.event.scanner.enabled:true}") boolean eventScannerEnabled,
      ScheduledExecutorService scheduler,
      TxEventRepository eventRepository,
      CommandRepository commandRepository,
      TxTimeoutRepository timeoutRepository,
      OmegaCallback omegaCallback,
      NodeStatus nodeStatus) {
    if (eventScannerEnabled) {
      new EventScanner(scheduler,
          eventRepository, commandRepository, timeoutRepository,
          omegaCallback, eventPollingInterval, nodeStatus).run();
      LOG.info("Starting the EventScanner.");
    }
    TxConsistentService consistentService = new TxConsistentService(eventRepository);
    return consistentService;
  }

  @Bean
  GrpcTxEventEndpointImpl grpcTxEventEndpoint(TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks) {
    ServerMeta serverMeta = ServerMeta.newBuilder()
        .putMeta(AlphaMetaKeys.AkkaEnabled.name(), String.valueOf(false)).build();
    return new GrpcTxEventEndpointImpl(txConsistentService, omegaCallbacks, serverMeta);
  }

  @Bean
  ClusterLockService clusterLockService() {
    return new ClusterLockService();
  }

  @Bean
  public MetricsService metricsService() {
    return new MetricsService();
  }

  @Bean
  AlphaMetricsEndpoint alphaMetricsEndpoint(){
    return new AlphaMetricsEndpointImpl();
  }

  @Bean
  SagaDbAPIv1Controller apIv1Controller(){
    return new SagaDbAPIv1Controller();
  }

  @Bean
  APIv1 apIv1(){
    return new SagaDbAPIv1Impl();
  }

  @Bean
  @Profile("test")
  AlphaEventController alphaEventController(TxEventEnvelopeRepository eventRepository){
    return new AlphaEventController(eventRepository);
  }
}