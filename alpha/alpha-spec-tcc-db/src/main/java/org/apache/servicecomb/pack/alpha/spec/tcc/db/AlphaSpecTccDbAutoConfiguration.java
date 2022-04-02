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

package org.apache.servicecomb.pack.alpha.spec.tcc.db;

import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.api.APIv1;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.api.TccDbAPIv1Controller;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.api.TccDbAPIv1Impl;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.OmegaCallbackWrapper;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.TccCallbackEngine;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.TccPendingTaskRunner;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.metrics.AlphaMetricsEndpointImpl;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.properties.SpecTccDbProperties;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.MemoryTxEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.RDBTxEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccEventScanner;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccTxEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccTxEventService;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.test.AlphaTccEventController;
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
@ImportAutoConfiguration({SpecTccDbProperties.class, EclipseLinkJpaConfiguration.class})
@ConditionalOnExpression("'${alpha.spec.names}'.contains('tcc-db')")
@EnableJpaRepositories(basePackages = "org.apache.servicecomb.pack.alpha.spec.tcc.db")
public class AlphaSpecTccDbAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Value("${alpha.tx.timeout-seconds:600}")
  private int globalTxTimeoutSeconds;

  public AlphaSpecTccDbAutoConfiguration() {
    LOG.info("Alpha Specification Tcc DB");
  }

  @Bean
  @Primary
  public DataSourceProperties dataSourceProperties(SpecTccDbProperties specTccDbProperties) {
    return specTccDbProperties.getDatasource();
  }

  @Bean
  TccTxEventRepository tccTxEventRepository(SpecTccDbProperties specTccDbProperties) {
    if (specTccDbProperties.isMemoryMode()) {
      return new MemoryTxEventRepository();
    } else {
      return new RDBTxEventRepository();
    }
  }

  @Bean
  TccCallbackEngine tccCallbackEngine() {
    return new TccCallbackEngine();
  }

  @Bean
  TccTxEventService tccTxEventService(TccTxEventRepository tccTxEventRepository,
      TccCallbackEngine tccCallbackEngine) {
    return new TccTxEventService(tccTxEventRepository, tccCallbackEngine);
  }

  @Bean
  TccPendingTaskRunner tccPendingTaskRunner() {
    return new TccPendingTaskRunner(delay);
  }

  @Bean
  OmegaCallbackWrapper omegaCallbackWrapper(TccPendingTaskRunner tccPendingTaskRunner) {
    return new OmegaCallbackWrapper(tccPendingTaskRunner);
  }

  @Bean
  GrpcTccEventService grpcTccEventService(TccTxEventService tccTxEventService,
      TccPendingTaskRunner tccPendingTaskRunner, TccEventScanner tccEventScanner) {
    // start the service which are needed for TCC
    tccPendingTaskRunner.start();
    tccEventScanner.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      tccPendingTaskRunner.shutdown();
      tccEventScanner.shutdown();
    }));
    return new GrpcTccEventService(tccTxEventService);
  }

  @Bean
  TccEventScanner tccEventScanner(TccTxEventService tccTxEventService) {
    return new TccEventScanner(tccTxEventService, delay, globalTxTimeoutSeconds);
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
  TccDbAPIv1Controller apIv1Controller(){
    return new TccDbAPIv1Controller();
  }

  @Bean
  APIv1 apIv1(){
    return new TccDbAPIv1Impl();
  }

  @Bean
  @Profile("test")
  AlphaTccEventController alphaTccEventController(TccTxEventRepository tccTxEventRepository){
    return new AlphaTccEventController(tccTxEventRepository);
  }
}