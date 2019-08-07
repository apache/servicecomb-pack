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

package org.apache.servicecomb.pack.alpha.fsm.repository;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.fsm.SagaActorState;
import org.apache.servicecomb.pack.alpha.core.fsm.TransactionType;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch.ElasticsearchTransactionRepository;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchTransactionRepositoryTest {

  @Mock
  ElasticsearchTemplate template;
  MetricsService metricsService;

  @Before
  public void before() {
    metricsService = new MetricsService();
  }

  @Test
  public void syncTest() throws Exception {
    int refreshTime = 0;
    int batchSize = 0;
    TransactionRepository repository = new ElasticsearchTransactionRepository(template,
        metricsService, batchSize, refreshTime);
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
          .state(SagaActorState.COMMITTED)
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
    int refreshTime = 2;
    int batchSize = 0;
    TransactionRepository repository = new ElasticsearchTransactionRepository(template,
        metricsService, batchSize, refreshTime * 1000);
    for (int i = 0; i < size; i++) {
      final String globalTxId = UUID.randomUUID().toString();
      GlobalTransaction transaction = GlobalTransaction.builder()
          .type(TransactionType.SAGA)
          .serviceName("serviceName")
          .instanceId("instanceId")
          .globalTxId(globalTxId)
          .beginTime(new Date())
          .endTime(new Date())
          .state(SagaActorState.COMMITTED)
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
    int refreshTime = 2;
    int batchSize = 10;
    TransactionRepository repository = new ElasticsearchTransactionRepository(template,
        metricsService, batchSize, refreshTime * 1000);
    for (int i = 0; i < size; i++) {
      final String globalTxId = UUID.randomUUID().toString();
      GlobalTransaction transaction = GlobalTransaction.builder()
          .type(TransactionType.SAGA)
          .serviceName("serviceName")
          .instanceId("instanceId")
          .globalTxId(globalTxId)
          .beginTime(new Date())
          .endTime(new Date())
          .state(SagaActorState.COMMITTED)
          .subTxSize(0)
          .subTransactions(new ArrayList<>())
          .events(new ArrayList<>())
          .build();
      repository.send(transaction);
    }
    await().atMost(refreshTime + 10, SECONDS).until(
        () -> metricsService.metrics().getRepositoryAccepted() == metricsService.metrics()
            .getRepositoryReceived());
    assertEquals(metricsService.metrics().getRepositoryAccepted(), size);

  }
}
