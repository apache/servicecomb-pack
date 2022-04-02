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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.ut.api;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.apache.servicecomb.pack.alpha.fsm.api.APIv1Controller;
import org.apache.servicecomb.pack.alpha.fsm.api.APIv1Impl;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.metrics.AlphaMetricsEndpointImpl;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@Configuration
public class TestConfiguration {
  @MockBean
  MetricsService metricsService;

  @MockBean
  NodeStatus nodeStatus;

  @MockBean
  ElasticsearchRestTemplate template;

  @MockBean
  TransactionRepository transactionRepository;

  @Bean
  AlphaMetricsEndpoint alphaMetricsEndpoint(){
    return new AlphaMetricsEndpointImpl();
  }

  @Bean
  APIv1Controller apIv1Controller(){
    return new APIv1Controller();
  }

  @Bean
  APIv1Impl apIv1(){
    return new APIv1Impl();
  }
}