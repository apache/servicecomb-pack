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

package org.apache.servicecomb.pack.alpha.server.fsm;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorSystem;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Map;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.fsm.SagaActorState;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension;
import org.apache.servicecomb.pack.alpha.server.AlphaApplication;
import org.apache.servicecomb.pack.alpha.server.AlphaConfig;
import org.apache.servicecomb.pack.common.EventType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8090",
        "alpha.event.pollingInterval=1",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.profiles.active=akka-persistence-mem",
        //akka
        "alpha.feature.akka.enabled=true",
        "alpha.feature.akka.channel.type=memory",
        "akkaConfig.akka.persistence.journal.plugin=akka.persistence.journal.inmem",
        "akkaConfig.akka.persistence.journal.leveldb.dir=target/example/journal",
        "akkaConfig.akka.persistence.snapshot-store.plugin=akka.persistence.snapshot-store.local",
        "akkaConfig.akka.persistence.snapshot-store.local.dir=target/example/snapshots",
        //elasticsearch
        "alpha.feature.akka.transaction.repository.channel.type=memory",
        "alpha.feature.akka.transaction.repository.type=elasticsearch",
        "spring.data.elasticsearch.cluster-name=elasticsearch",
        "spring.data.elasticsearch.cluster-nodes=localhost:9300",
        "spring.elasticsearch.rest.uris=http://localhost:9200"
       })
public class AlphaIntegrationFsmTest {
  private static final OmegaEventSender omegaEventSender = OmegaEventSender.builder().build();
  private static final int port = 8090;

  @Autowired(required = false)
  ActorSystem system;

  @Autowired
  private Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  @MockBean
  ElasticsearchTemplate elasticsearchTemplate;

  @BeforeClass
  public static void beforeClass() {
    omegaEventSender.configClient(NettyChannelBuilder.forAddress("0.0.0.0", port).usePlaintext().build());
  }

  @AfterClass
  public static void afterClass() throws Exception {
    omegaEventSender.shutdown();
  }

  @Before
  public void before() {
    omegaEventSender.setOmegaCallbacks(omegaCallbacks);
    omegaEventSender.reset();
  }

  @After
  public void after() {
    SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).cleanLastSagaData();
    omegaEventSender.onDisconnected();
  }

  @Test
  public void successfulTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.onConnected();
    omegaEventSender.getOmegaEventSagaSimulator().sagaSuccessfulEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void firstTxAbortedEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().firstTxAbortedEvents(globalTxId, localTxId_1).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),1);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.FAILED);
  }

  @Test
  public void middleTxAbortedEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().middleTxAbortedEvents(globalTxId, localTxId_1, localTxId_2).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),2);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.FAILED);
  }

  @Test
  public void lastTxAbortedEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.FAILED);
    assertArrayEquals(sagaData.getTxEntityMap().get(localTxId_3).getThrowablePayLoads(),NullPointerException.class.getName().getBytes());
  }

  @Test
  public void receivedRemainingEventAfterFirstTxAbortedEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().receivedRemainingEventAfterFirstTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(5, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.FAILED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMPENSATED);
  }

  @Test
  public void receivedRemainingEventAndDelayLastTxEventAfterFirstTxAbortedEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().receivedRemainingEventAfterFirstTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      if(event.getType().equals(EventType.TxStartedEvent.name()) && event.getLocalTxId().equals(localTxId_3)){
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(5, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.FAILED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMPENSATED);
  }

  @Test
  public void sagaAbortedEventAfterAllTxEndedTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().sagaAbortedEventAfterAllTxEndedsEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(20, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMPENSATED);
  }

  @Test
  public void omegaSendSagaTimeoutEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().omegaSendSagaTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.SUSPENDED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void sagaActorTriggerTimeoutTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    final int timeout = 5; // second
    omegaEventSender.getOmegaEventSagaSimulator().sagaActorTriggerTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3, timeout).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(timeout + 1, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.SUSPENDED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void successfulWithTxConcurrentTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().successfulWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void successfulWithTxConcurrentCrossTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().successfulWithTxConcurrentCrossEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void lastTxAbortedEventWithTxConcurrentTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().lastTxAbortedEventWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.FAILED);
  }

  @Test
  public void doNotCompensateDuplicateTxOnFailure() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().duplicateTxOnFailureEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.FAILED);
  }

  @Test
  public void compensateOmegaDisconnectTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    final Map<String, OmegaCallback>[] omegaInstance = new Map[]{null};
    final String[] serviceName = new String[1];
    omegaEventSender.getOmegaEventSagaSimulator().lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      if(event.getType().equals(EventType.TxAbortedEvent.name())){
        //simulate omega disconnect
        serviceName[0] = event.getServiceName();
        omegaInstance[0] = omegaEventSender.getOmegaCallbacks().get(event.getServiceName());
        omegaEventSender.getOmegaCallbacks().remove(event.getServiceName());
      }
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    //simulate omega connected
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.getTxEntityMap().size()==3;
    });
    omegaEventSender.getOmegaCallbacks().put(serviceName[0], omegaInstance[0]);

    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.FAILED);
    assertArrayEquals(sagaData.getTxEntityMap().get(localTxId_3).getThrowablePayLoads(),NullPointerException.class.getName().getBytes());
  }

  @Test
  public void compensateFailRetryOnce() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(2, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(),TxState.COMPENSATED);
    assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(),TxState.FAILED);
    assertArrayEquals(sagaData.getTxEntityMap().get(localTxId_3).getThrowablePayLoads(),NullPointerException.class.getName().getBytes());
  }
}
