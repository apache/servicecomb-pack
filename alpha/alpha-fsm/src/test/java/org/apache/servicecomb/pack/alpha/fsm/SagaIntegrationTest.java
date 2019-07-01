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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import akka.actor.ActorSystem;
import com.google.common.eventbus.EventBus;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.LogExtension;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SagaApplication.class},
    properties = {
        "alpha.model.actor.enabled=true",
        "spring.profiles.active=akka-persistence-redis"
    })
public class SagaIntegrationTest {

  @Autowired
  @Qualifier("sagaEventBus")
  EventBus sagaEventBus;

  @Autowired
  ActorSystem system;

  @Test
  public void successfulTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.successfulEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMMITTED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMMITTED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void firstTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    SagaEventSender.firstTxAbortedEvents(globalTxId, localTxId_1).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 1
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.FAILED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void middleTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    SagaEventSender.middleTxAbortedEvents(globalTxId, localTxId_1, localTxId_2).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 2
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.FAILED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void lastTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.lastTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.FAILED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void sagaAbortedEventBeforeTxComponsitedEventTest() {
      final String globalTxId = UUID.randomUUID().toString();
      final String localTxId_1 = UUID.randomUUID().toString();
      final String localTxId_2 = UUID.randomUUID().toString();
      final String localTxId_3 = UUID.randomUUID().toString();
      SagaEventSender.sagaAbortedEventBeforeTxComponsitedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
        sagaEventBus.post(event);
      });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.FAILED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void receivedRemainingEventAfterFirstTxAbortedEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.receivedRemainingEventAfterFirstTxAbortedEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.FAILED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMPENSATED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void sagaAbortedEventAfterAllTxEndedTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.sagaAbortedEventAfterAllTxEndedsEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMPENSATED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void omegaSendSagaTimeoutEventTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.omegaSendSagaTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.SUSPENDED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMMITTED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void sagaActorTriggerTimeoutTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    final int timeout = 5; // second
    SagaEventSender.sagaActorTriggerTimeoutEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3, timeout).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(timeout+1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.SUSPENDED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMMITTED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void successfulWithTxConcurrentTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.successfulWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMMITTED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMMITTED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void successfulWithTxConcurrentCrossTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.successfulWithTxConcurrentCrossEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMMITTED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMMITTED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.COMMITTED;
      }else{
        return false;
      }
    });
  }

  @Test
  public void lastTxAbortedEventWithTxConcurrentTest() {
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();
    SagaEventSender.lastTxAbortedEventWithTxConcurrentEvents(globalTxId, localTxId_1, localTxId_2, localTxId_3).stream().forEach( event -> {
      sagaEventBus.post(event);
    });

    await().atMost(1, SECONDS).until(() -> {
      SagaData sagaData = LogExtension.LogExtensionProvider.get(system).getSagaData(globalTxId);
      if(sagaData != null){
        return sagaData.getLastState() == SagaActorState.COMPENSATED
            && sagaData.getBeginTime() > 0
            && sagaData.getEndTime() >0
            && sagaData.getTxEntityMap().size() == 3
            && sagaData.getTxEntityMap().get(localTxId_1).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_2).getState() == TxState.COMPENSATED
            && sagaData.getTxEntityMap().get(localTxId_3).getState() == TxState.FAILED;
      }else{
        return false;
      }
    });
  }

}
