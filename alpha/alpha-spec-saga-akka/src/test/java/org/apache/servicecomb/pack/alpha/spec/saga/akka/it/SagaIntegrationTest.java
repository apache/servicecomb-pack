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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorSystem;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.SagaActorState;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.util.SagaEventSender;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.memory.MemoryActorEventChannel;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.SagaData;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka.SagaDataExtension;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SagaApplication.class},
    properties = {
        // saga-akka
        "alpha.spec.names=saga-akka",
        // akka
        "akkaConfig.akka.persistence.journal.plugin=akka.persistence.journal.inmem",
        "akkaConfig.akka.persistence.journal.leveldb.dir=target/example/journal",
        "akkaConfig.akka.persistence.snapshot-store.plugin=akka.persistence.snapshot-store.local",
        "akkaConfig.akka.persistence.snapshot-store.local.dir=target/example/snapshots",
        // channel
        "alpha.spec.saga.akka.channel.name=memory",
        "alpha.spec.saga.akka.channel.memory.max-length=-1",
        // elasticsearch
        "alpha.spec.saga.akka.repository.name=elasticsearch",
        "alpha.spec.saga.akka.repository.elasticsearch.uris=localhost:9300",
        "alpha.spec.saga.akka.repository.elasticsearch.batch-size=0",
        "alpha.spec.saga.akka.repository.elasticsearch.refresh-time=5000"
    })
public class SagaIntegrationTest {

  @Autowired
  ActorSystem system;
  
  @Autowired
  MemoryActorEventChannel memoryActorEventChannel;

  @Autowired
  MetricsService metricsService;

  @MockBean
  ElasticsearchRestTemplate elasticsearchRestTemplate;

  @Before
  public void before(){
    when(elasticsearchRestTemplate.indexOps(ArgumentMatchers.any(IndexCoordinates.class))).thenReturn(mock(
        IndexOperations.class));
  }

  @After
  public void after(){
    SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).cleanLastSagaData();
  }

  @Test
  public void successfulTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.successfulEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system)
          .getLastSagaData();
      return sagaData != null && sagaData.isTerminated()
          && sagaData.getLastState() == SagaActorState.COMMITTED
          && metricsService.metrics().getSagaEndCounter() == 1;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
    assertEquals(metricsService.metrics().getActorReceived(),8);
    assertEquals(metricsService.metrics().getActorAccepted(),8);
    assertEquals(metricsService.metrics().getSagaBeginCounter(),1);
    assertEquals(metricsService.metrics().getSagaEndCounter(),1);
  }

  @Test
  public void firstTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    SagaEventSender.firstTxAbortedEvents(globalTxId, localTxId_1).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });

    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),1);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.FAILED);
  }

  @Test
  public void middleTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    SagaEventSender.middleTxAbortedEvents(globalTxId, localTxId_1, localTxId_2).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),2);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.FAILED);
  }

  @Test
  public void lastTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
  }

  @Test
  public void sagaAbortedEventBeforeTxComponsitedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.sagaAbortedEventBeforeTxComponsitedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
  }

  @Test
  public void receivedRemainingEventAfterFirstTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.receivedRemainingEventAfterFirstTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.FAILED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMPENSATED_SUCCEED);
  }

  @Test
  public void sagaAbortedEventAfterAllTxEndedTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.sagaAbortedEventAfterAllTxEndedsEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMPENSATED_SUCCEED);
  }

  @Test
  public void omegaSendSagaTimeoutEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.omegaSendSagaTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.SUSPENDED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void sagaActorTriggerTimeoutTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    final int timeout = 5; // second
    SagaEventSender.sagaActorTriggerTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3, timeout).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(timeout + 120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.SUSPENDED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void successfulWithTxConcurrentTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.successfulWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void successfulWithTxConcurrentCrossTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.successfulWithTxConcurrentCrossEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void lastTxAbortedEventWithTxConcurrentTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.lastTxAbortedEventWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      memoryActorEventChannel.send(event);
    });
    await().atMost(120, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertEquals(sagaData.getTxEntities().size(),3);
    await().atMost(120, SECONDS).until(() -> {
      return sagaData.getTxEntities().get(localTxId_1) != null
          && sagaData.getTxEntities().get(localTxId_2) != null
          && sagaData.getTxEntities().get(localTxId_3) != null;
    });
    assertEquals(TxState.COMPENSATED_SUCCEED, sagaData.getTxEntities().get(localTxId_1).getState());
    assertEquals(TxState.COMPENSATED_SUCCEED, sagaData.getTxEntities().get(localTxId_2).getState());
    assertEquals(TxState.FAILED, sagaData.getTxEntities().get(localTxId_3).getState());
  }

}
