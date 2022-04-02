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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.ut.repository;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.fsm.TransactionType;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.SagaActorState;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.properties.ElasticsearchProperties;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.repository.elasticsearch.ElasticsearchTransactionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchTransactionRepositoryTest {

  @Mock
  ElasticsearchRestTemplate template;
  MetricsService metricsService;

  @Before
  public void before() {
    when(template.indexOps(ArgumentMatchers.any(IndexCoordinates.class))).thenReturn(mock(
        IndexOperations.class));
    metricsService = new MetricsService();
  }

  @Test
  public void syncTest() throws Exception {
    ElasticsearchProperties elasticsearchProperties = new ElasticsearchProperties();
    elasticsearchProperties.setBatchSize(0);
    elasticsearchProperties.setRefreshTime(0);
    TransactionRepository repository = new ElasticsearchTransactionRepository(elasticsearchProperties, template, metricsService);
    int size = 100;
    for (int i = 0; i < size; i++) {
      final String globalTxId = UUID.randomUUID().toString();
      GlobalTransaction transaction = GlobalTransaction.builder()
          .type(TransactionType.SAGA)
          .serviceName("serviceName")
          .instanceId("instanceId")
          .globalTxId(globalTxId)
          .beginTime(new Date())
          .endTime(new Date())
          .state(SagaActorState.COMMITTED.name())
          .subTxSize(0)
          .subTransactions(new ArrayList<>())
          .events(new ArrayList<>())
          .build();
      repository.send(transaction);
    }
    assertEquals(metricsService.metrics().getRepositoryAccepted(), size);
    assertEquals(metricsService.metrics().getRepositoryAccepted(),
        metricsService.metrics().getRepositoryReceived());

  }


  @Test
  public void syncWithRefreshTimeTest() throws Exception {
    int size = 100;
    ElasticsearchProperties elasticsearchProperties = new ElasticsearchProperties();
    elasticsearchProperties.setBatchSize(0);
    elasticsearchProperties.setRefreshTime(2000);
    TransactionRepository repository = new ElasticsearchTransactionRepository(elasticsearchProperties,
        template, metricsService);
    for (int i = 0; i < size; i++) {
      final String globalTxId = UUID.randomUUID().toString();
      GlobalTransaction transaction = GlobalTransaction.builder()
          .type(TransactionType.SAGA)
          .serviceName("serviceName")
          .instanceId("instanceId")
          .globalTxId(globalTxId)
          .beginTime(new Date())
          .endTime(new Date())
          .state(SagaActorState.COMMITTED.name())
          .subTxSize(0)
          .subTransactions(new ArrayList<>())
          .events(new ArrayList<>())
          .build();
      repository.send(transaction);
    }
    assertEquals(metricsService.metrics().getRepositoryAccepted(), size);
    assertEquals(metricsService.metrics().getRepositoryAccepted(),
        metricsService.metrics().getRepositoryReceived());

  }

  @Test
  public void asyncWithBatchSizeAndRefreshTimeTest() throws Exception {
    int size = 15;
    ElasticsearchProperties elasticsearchProperties = new ElasticsearchProperties();
    elasticsearchProperties.setBatchSize(10);
    elasticsearchProperties.setRefreshTime(2000);
    TransactionRepository repository = new ElasticsearchTransactionRepository(elasticsearchProperties,
        template, metricsService);
    for (int i = 0; i < size; i++) {
      final String globalTxId = UUID.randomUUID().toString();
      GlobalTransaction transaction = GlobalTransaction.builder()
          .type(TransactionType.SAGA)
          .serviceName("serviceName")
          .instanceId("instanceId")
          .globalTxId(globalTxId)
          .beginTime(new Date())
          .endTime(new Date())
          .state(SagaActorState.COMMITTED.name())
          .subTxSize(0)
          .subTransactions(new ArrayList<>())
          .events(new ArrayList<>())
          .build();
      repository.send(transaction);
    }
    await().atMost(2000 + 10, SECONDS).until(
        () -> metricsService.metrics().getRepositoryAccepted() == metricsService.metrics()
            .getRepositoryReceived());
    assertEquals(metricsService.metrics().getRepositoryAccepted(), size);

  }
}
