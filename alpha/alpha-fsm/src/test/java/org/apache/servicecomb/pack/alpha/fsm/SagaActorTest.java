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

package org.apache.servicecomb.pack.alpha.fsm;

import static org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.persistence.fsm.PersistentFSM;
import akka.persistence.fsm.PersistentFSM.CurrentState;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.repository.channel.DefaultTransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch.ElasticsearchTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import static org.hamcrest.CoreMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class SagaActorTest {

  static ActorSystem system;

  @Mock
  ElasticsearchTemplate template;

  static MetricsService metricsService = new MetricsService();

  private static Map<String,Object> getPersistenceMemConfig(){
    Map<String, Object> map = new HashMap<>();
    map.put("akka.persistence.journal.plugin", "akka.persistence.journal.inmem");
    map.put("akka.persistence.journal.leveldb.dir", "target/example/journal");
    map.put("akka.persistence.snapshot-store.plugin", "akka.persistence.snapshot-store.local");
    map.put("akka.persistence.snapshot-store.local.dir", "target/example/snapshots");
    return map;
  }

  private static Map<String,Object> getPersistenceRedisConfig(){
    Map<String, Object> map = new HashMap<>();
    map.put("akka.actor.warn-about-java-serializer-usage",false);
    map.put("akka.persistence.journal.plugin", "akka-persistence-redis.journal");
    map.put("akka.persistence.snapshot-store.plugin", "akka-persistence-redis.snapshot");
    map.put("akka-persistence-redis.redis.mode", "simple");
    map.put("akka-persistence-redis.redis.host", "localhost");
    map.put("akka-persistence-redis.redis.port", "6379");
    map.put("akka-persistence-redis.redis.database", "0");
    return map;
  }

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create("SagaActorTest", ConfigFactory.parseMap(getPersistenceMemConfig()));
    SAGA_DATA_EXTENSION_PROVIDER.get(system).setMetricsService(metricsService);
  }

  @AfterClass
  public static void tearDown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Before
  public void before(){
    TransactionRepository repository = new ElasticsearchTransactionRepository(template, metricsService, 0,0);
    TransactionRepositoryChannel repositoryChannel = new DefaultTransactionRepositoryChannel(repository, metricsService);
    SAGA_DATA_EXTENSION_PROVIDER.get(system).setRepositoryChannel(repositoryChannel);
  }

  @After
  public void after(){
    SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(system).cleanLastSagaData();
  }

  public String genPersistenceId() {
    return UUID.randomUUID().toString();
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 4. TxStartedEvent-13
   * 5. TxEndedEvent-13
   * 6. SagaEndedEvent-1
   */
  @Test
  public void successfulTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.successfulEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      sagaData.getTxEntities().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });
      assertThat(eventList, is(sagaData.getEvents()));
      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 4. TxStartedEvent-13
   * 5. TxEndedEvent-13
   * 6. SagaEndedEvent-1
   */
  @Test
  public void successfulRecoveryWithCorrectStateDataTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      String persistenceId = genPersistenceId();
      ActorRef saga = system.actorOf(SagaActor.props(persistenceId));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      List<BaseEvent> eventListFirst = SagaEventSender.successfulFirstHalfEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventListFirst.stream().forEach(event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE,
          SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED,
          SagaActorState.PARTIALLY_ACTIVE);

      //expectTerminated(fsm);
      ActorRef recoveredSaga = system.actorOf(SagaActor.props(persistenceId), "recoveredSaga");
      watch(recoveredSaga);
      recoveredSaga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      List<BaseEvent> eventListSecond = SagaEventSender.successfulSecondHalfEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventListSecond.stream().forEach(event -> {
        recoveredSaga.tell(event, getRef());
      });

      currentState = expectMsgClass(akka.persistence.fsm.PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.PARTIALLY_ACTIVE, currentState.state());

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_ACTIVE,
          SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_COMMITTED,
          SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_ACTIVE,
          SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_COMMITTED,
          SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), recoveredSaga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      sagaData.getTxEntities().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });
      eventListFirst.addAll(eventListSecond);
      assertThat(eventListFirst, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxAbortedEvent-11
   * 7. SagaAbortedEvent-1
   */
  @Test
  public void firstTxAbortedEventTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      List<BaseEvent> eventList = SagaEventSender.firstTxAbortedEvents(globalTxId, localTxId_1);
      eventList.forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 1);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }


  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxAbortedEvent-12
   * 6. TxCompensatedEvent-11
   * 7. SagaAbortedEvent-1
   */
  @Test
  public void middleTxAbortedEventTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.middleTxAbortedEvents(globalTxId, localTxId_1, localTxId_2);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 2);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxAbortedEvent-13
   * 8. TxCompensatedEvent-11
   * 9. TxCompensatedEvent-12
   * 10. SagaAbortedEvent-1
   */
  @Test
  public void lastTxAbortedEventTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxAbortedEvent-13
   * 8. TxCompensatedEvent-11
   * 9. TxCompensatedEvent-12
   * 10. SagaAbortedEvent-1
   */
  @Test
  public void sagaAbortedEventBeforeTxComponsitedEventTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.sagaAbortedEventBeforeTxComponsitedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxAbortedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxEndedEvent-13
   * 8. TxCompensatedEvent-12
   * 9. TxCompensatedEvent-13
   * 10. SagaAbortedEvent-1
   */
  @Test
  public void receivedRemainingEventAfterFirstTxAbortedEventTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.receivedRemainingEventAfterFirstTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.FAILED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }


  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxEndedEvent-13
   * 8. SagaAbortedEvent-1
   * 9. TxCompensatedEvent-11
   * 8. TxCompensatedEvent-12
   * 9. TxCompensatedEvent-13
   */
  @Test
  public void sagaAbortedEventAfterAllTxEndedTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.sagaAbortedEventAfterAllTxEndedsEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }


  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 4. TxStartedEvent-13
   * 5. TxEndedEvent-13
   * 5. SagaTimeoutEvent-1
   */
  @Test
  public void omegaSendSagaTimeoutEventTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.omegaSendSagaTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.SUSPENDED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMMITTED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.COMMITTED);
      assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(), TxState.COMMITTED);
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent(5s)-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 4. TxStartedEvent-13
   * 5. TxEndedEvent-13
   */
  @Test
  public void sagaActorTriggerTimeoutTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();
      final int timeout = 5;

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

      List<BaseEvent> eventList = SagaEventSender.sagaActorTriggerTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3, timeout);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());
      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(Duration.ofSeconds(timeout+2),PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.SUSPENDED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  // tx concurrent execution

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxStartedEvent-12
   * 4. TxStartedEvent-13
   * 5. TxEndedEvent-11
   * 6. TxEndedEvent-12
   * 7. TxEndedEvent-13
   * 8. SagaEndedEvent-1
   */
  @Test
  public void successfulWithTxConcurrentTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      List<BaseEvent> eventList = SagaEventSender.successfulWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      sagaData.getTxEntities().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxStartedEvent-12
   * 5. TxEndedEvent-11
   * 4. TxStartedEvent-13
   * 6. TxEndedEvent-12
   * 7. TxEndedEvent-13
   * 8. SagaEndedEvent-1
   */
  @Test
  public void successfulWithTxConcurrentCrossTest() throws InterruptedException {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      List<BaseEvent> eventList = SagaEventSender.successfulWithTxConcurrentCrossEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      sagaData.getTxEntities().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 4. TxStartedEvent-12
   * 6. TxStartedEvent-13
   * 3. TxEndedEvent-11
   * 5. TxEndedEvent-12
   * 7. TxAbortedEvent-13
   * 8. TxCompensatedEvent-11
   * 9. TxCompensatedEvent-12
   * 10. SagaAbortedEvent-1
   */
  @Test
  public void lastTxAbortedEventWithTxConcurrentTest() {
    new TestKit(system) {{
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();

      ActorRef saga = system.actorOf(SagaActor.props(genPersistenceId()));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      List<BaseEvent> eventList = SagaEventSender.lastTxAbortedEventWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3);
      eventList.stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDLE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDLE, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.FAILED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      SagaData sagaData = SAGA_DATA_EXTENSION_PROVIDER.get(system).getLastSagaData();
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntities().size(), 3);
      assertEquals(sagaData.getTxEntities().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntities().get(localTxId_3).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);
      assertThat(eventList, is(sagaData.getEvents()));

      system.stop(saga);
    }};
  }

  private static void assertSagaTransition(PersistentFSM.Transition transition, ActorRef actorRef,
      SagaActorState from, SagaActorState to) {
    assertEquals(transition.fsmRef(), actorRef);
    assertEquals(transition.from(), from);
    assertEquals(transition.to(), to);
  }

}
