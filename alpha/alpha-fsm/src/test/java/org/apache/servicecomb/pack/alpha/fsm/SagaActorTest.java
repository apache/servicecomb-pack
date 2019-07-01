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
import java.util.Map;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.LogExtension;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SagaActorTest {

  static ActorSystem system;

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
  }

  @AfterClass
  public static void tearDown() {
    TestKit.shutdownActorSystem(system);
    system = null;
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

      SagaEventSender.successfulEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

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

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      sagaData.getTxEntityMap().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
      SagaEventSender.successfulFirstHalfEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      //expectTerminated(saga);

      ActorRef recoveredSaga = system.actorOf(SagaActor.props(persistenceId), "recoveredSaga");
      watch(recoveredSaga);
      recoveredSaga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      SagaEventSender.successfulSecondHalfEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        recoveredSaga.tell(event, getRef());
      });

      currentState = expectMsgClass(akka.persistence.fsm.PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.PARTIALLY_ACTIVE, currentState.state());

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      sagaData.getTxEntityMap().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), recoveredSaga);
      system.stop(saga);
    }};
  }

  @Test
  public void successfulRecoveryWithCorrectStateDataTestAndDiffentSystem() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    String persistenceId = genPersistenceId();
    ActorSystem system1 = ActorSystem.create("SagaActorTest1", ConfigFactory.parseMap(getPersistenceRedisConfig()));
    new TestKit(system1) {{
      ActorRef saga = system1.actorOf(SagaActor.props(persistenceId));
      watch(saga);
      saga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      SagaEventSender.successfulFirstHalfEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);
      system1.stop(saga);
    }};
    TestKit.shutdownActorSystem(system1);

    ActorSystem system2 = ActorSystem.create("SagaActorTest2", ConfigFactory.parseMap(getPersistenceRedisConfig()));
    new TestKit(system2) {{
      ActorRef recoveredSaga = system2.actorOf(SagaActor.props(persistenceId), "recoveredSaga");
      watch(recoveredSaga);
      recoveredSaga.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());
      SagaEventSender.successfulSecondHalfEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        recoveredSaga.tell(event, getRef());
      });

      CurrentState currentState = expectMsgClass(akka.persistence.fsm.PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.PARTIALLY_ACTIVE, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      sagaData.getTxEntityMap().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, recoveredSaga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), recoveredSaga);
      system2.stop(recoveredSaga);
    }};
    TestKit.shutdownActorSystem(system2);
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

      SagaEventSender.firstTxAbortedEvents(globalTxId, localTxId_1).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 1);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      system.stop(saga);
    }};
  }


  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxAbortedEvent-12
   * 6. TxComponsitedEvent-11
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

      SagaEventSender.middleTxAbortedEvents(globalTxId, localTxId_1, localTxId_2).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 2);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
   * 8. TxComponsitedEvent-11
   * 9. TxComponsitedEvent-12
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

      SagaEventSender.lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

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

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
   * 8. TxComponsitedEvent-11
   * 9. TxComponsitedEvent-12
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

      SagaEventSender.sagaAbortedEventBeforeTxComponsitedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

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

      SagaData sagaData = expectMsgClass(SagaData.class);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

      sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);//expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

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
   * 8. TxComponsitedEvent-12
   * 9. TxComponsitedEvent-13
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

      SagaEventSender.receivedRemainingEventAfterFirstTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.FAILED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.FAILED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
   * 9. TxComponsitedEvent-11
   * 8. TxComponsitedEvent-12
   * 9. TxComponsitedEvent-13
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

      SagaEventSender.sagaAbortedEventAfterAllTxEndedsEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

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

      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

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

      SagaEventSender.omegaSendSagaTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

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

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.COMMITTED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.COMMITTED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(), TxState.COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.SUSPENDED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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

      SagaEventSender.sagaActorTriggerTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3, timeout).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());
      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

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
      SagaEventSender.successfulWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      sagaData.getTxEntityMap().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
      SagaEventSender.successfulWithTxConcurrentCrossEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      sagaData.getTxEntityMap().forEach((k, v) -> {
        assertEquals(v.getState(), TxState.COMMITTED);
      });

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.COMMITTED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
   * 8. TxComponsitedEvent-11
   * 9. TxComponsitedEvent-12
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
      SagaEventSender.lastTxAbortedEventWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        saga.tell(event, getRef());
      });

      //expect
      CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
      assertEquals(SagaActorState.IDEL, currentState.state());

      PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.IDEL, SagaActorState.READY);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.READY, SagaActorState.PARTIALLY_ACTIVE);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_ACTIVE, SagaActorState.PARTIALLY_COMMITTED);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.PARTIALLY_COMMITTED, SagaActorState.FAILED);

      SagaData sagaData = expectMsgClass(SagaData.class);
      assertEquals(sagaData.getGlobalTxId(), globalTxId);
      assertEquals(sagaData.getTxEntityMap().size(), 3);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_1).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_2).getState(), TxState.COMPENSATED);
      assertEquals(sagaData.getTxEntityMap().get(localTxId_3).getState(), TxState.FAILED);
      assertEquals(sagaData.getCompensationRunningCounter().intValue(), 0);

      transition = expectMsgClass(PersistentFSM.Transition.class);
      assertSagaTransition(transition, saga, SagaActorState.FAILED, SagaActorState.COMPENSATED);

      Terminated terminated = expectMsgClass(Terminated.class);
      assertEquals(terminated.getActor(), saga);

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
