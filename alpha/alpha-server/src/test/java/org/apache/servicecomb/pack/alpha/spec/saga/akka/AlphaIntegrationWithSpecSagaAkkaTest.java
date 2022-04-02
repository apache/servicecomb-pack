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

package org.apache.servicecomb.pack.alpha.spec.saga.akka;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorSystem;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.SagaData;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka.SagaDataExtension;
import org.apache.servicecomb.pack.alpha.server.AlphaApplication;
import org.apache.servicecomb.pack.alpha.server.AlphaConfig;
import org.apache.servicecomb.pack.common.AlphaMetaKeys;
import org.apache.servicecomb.pack.common.EventType;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8090",
        "alpha.event.pollingInterval=1",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.profiles.active=akka-persistence-mem",
        // saga-akka
        "alpha.spec.names=saga-akka",
        "alpha.spec.saga.akka.channel.name=memory",
        "alpha.spec.saga.akka.channel.memory.max-length=-1",
        "alpha.spec.saga.akka.repository.name=elasticsearch",
        "alpha.spec.saga.akka.repository.elasticsearch.uris=http://localhost:9200",
        // akka
        "akkaConfig.akka.persistence.journal.plugin=akka.persistence.journal.inmem",
        "akkaConfig.akka.persistence.journal.leveldb.dir=target/example/journal",
        "akkaConfig.akka.persistence.snapshot-store.plugin=akka.persistence.snapshot-store.local",
        "akkaConfig.akka.persistence.snapshot-store.local.dir=target/example/snapshots"
       })
public class AlphaIntegrationWithSpecSagaAkkaTest {
  private static final OmegaEventSender omegaEventSender = OmegaEventSender.builder().build();
  private static final int port = 8090;

  @Autowired(required = false)
  ActorSystem system;

  @Autowired
  private Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  @MockBean
  ElasticsearchRestTemplate elasticsearchRestTemplate;

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
  public void serverMetaTest(){
    ServerMeta serverMeta = omegaEventSender.onGetServerMeta();
    assertEquals(Boolean.parseBoolean(serverMeta.getMetaMap().get(AlphaMetaKeys.AkkaEnabled.name())),true);
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
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
  }

  @Test
  public void firstTxAbortedEventTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator().firstTxAbortedEvents(globalTxId, localTxId_1).stream().forEach( event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),1);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.FAILED);
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_1);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),2);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.FAILED);
  }

  @Test
  public void middleTxAbortedEventAndCompensationTimeoutTest() {
    omegaEventSender.onConnected();
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    omegaEventSender.getOmegaEventSagaSimulator()
        .middleTxAbortedEventAndCompensationTimeoutEvents(globalTxId, localTxId_1, localTxId_2).stream().forEach(event -> {
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system)
          .getLastSagaData();
      return sagaData != null && sagaData.isTerminated()
          && sagaData.getLastState() == SagaActorState.SUSPENDED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system)
        .getLastSagaData();
    assertEquals(sagaData.getLastState(), SagaActorState.SUSPENDED);
    assertEquals(sagaData.getTxEntities().size(), 2);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMPENSATED_FAILED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.FAILED);
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_1, localTxId_2);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
    assertArrayEquals(sagaData.getTxEntities().get(localTxId_3).getThrowablePayLoads(),NullPointerException.class.getName().getBytes());
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_2, localTxId_3);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.FAILED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMPENSATED_SUCCEED);
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_2, localTxId_3);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.FAILED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMPENSATED_SUCCEED);
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_1, localTxId_2, localTxId_3);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMPENSATED_SUCCEED);
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
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.SUSPENDED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
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
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
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
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
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
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMMITTED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMMITTED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.COMMITTED);
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_1, localTxId_2);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_1, localTxId_2);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
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
    omegaEventSender.getOmegaEventSagaSimulator().lastTxAbortedEventsAndReverseRetries(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      if(event.getType().equals(EventType.TxAbortedEvent.name())){
        //simulate omega disconnect
        serviceName[0] = event.getServiceName();
        omegaInstance[0] = omegaEventSender.getOmegaCallbacks().get(event.getServiceName());
        omegaEventSender.getOmegaCallbacks().remove(event.getServiceName());
      }
      omegaEventSender.getBlockingStub().onTxEvent(event);
    });
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.getTxEntities().size()==3;
    });

    //simulate omega connected
    omegaEventSender.getOmegaCallbacks().put(serviceName[0], omegaInstance[0]);

    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      if(sagaData != null){
        if(sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED){
          return true;
        }else{
          sagaData.getTxEntities().forEachReverse((k,v) -> {
            if(v.getState() == TxState.COMPENSATION_SENT || v.getState() == TxState.COMPENSATED_FAILED){
              omegaEventSender.getBlockingStub().onTxEvent(omegaEventSender.getOmegaEventSagaSimulator().getTxCompensateAckSucceedEvent(v.getGlobalTxId(),v.getLocalTxId()));
            }
          });
          return false;
        }
      } else {
        return false;
      }
    });

    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
    assertArrayEquals(sagaData.getTxEntities().get(localTxId_3).getThrowablePayLoads(),NullPointerException.class.getName().getBytes());
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
    waitAlphaCallCompensate(omegaEventSender, globalTxId, localTxId_1, localTxId_2);
    await().atMost(30, SECONDS).until(() -> {
      SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      return sagaData !=null && sagaData.isTerminated() && sagaData.getLastState()==SagaActorState.COMPENSATED;
    });
    SagaData sagaData = SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
    assertEquals(sagaData.getLastState(),SagaActorState.COMPENSATED);
    assertEquals(sagaData.getTxEntities().size(),3);
    assertNotNull(sagaData.getBeginTime());
    assertNotNull(sagaData.getEndTime());
    assertTrue(sagaData.getEndTime().getTime() > sagaData.getBeginTime().getTime());
    assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(),TxState.COMPENSATED_SUCCEED);
    assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(),TxState.FAILED);
    assertArrayEquals(sagaData.getTxEntities().get(localTxId_3).getThrowablePayLoads(),NullPointerException.class.getName().getBytes());
  }

  /**
   * Send compensation result event after Alpha calls compensation method
   * */
  private void waitAlphaCallCompensate(OmegaEventSender omegaEventSender,String globalTxId, String...localTxIds){
    OmegaCallback omegaCallback = omegaCallbacks.get(omegaEventSender.getServiceConfig().getServiceName())
        .get(omegaEventSender.getServiceConfig().getInstanceId());
    Arrays.asList(localTxIds).stream().forEach( localTxId_X -> {
      await().atMost(30, SECONDS)
          .until(() -> omegaCallback.isWaiting());
      omegaEventSender.getBlockingStub().onTxEvent(omegaEventSender.getOmegaEventSagaSimulator().getTxCompensateAckSucceedEvent(globalTxId,localTxId_X));
    });
  }
}
