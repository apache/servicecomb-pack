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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaTimeoutEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckFailedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckSucceedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;

/**
 * Event simulator
 * */

public class SagaEventSender {

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
  public static List<BaseEvent> successfulEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxAbortedEvent-11
   * 7. SagaAbortedEvent-1
   */
  public static List<BaseEvent> firstTxAbortedEvents(String globalTxId, String localTxId_1){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxAbortedEvent-12
   * 6. TxCompensateAckSucceedEvent-11
   * 7. SagaAbortedEvent-1
   */
  public static List<BaseEvent> middleTxAbortedEvents(String globalTxId, String localTxId_1, String localTxId_2){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxAbortedEvent-13
   * 8. TxCompensateAckSucceedEvent-11
   * 9. TxCompensateAckSucceedEvent-12
   * 10. SagaAbortedEvent-1
   */
  public static List<BaseEvent> lastTxAbortedEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxAbortedEvent-12
   * 6. TxCompensateAckFailedEvent-11
   * 7. TxCompensateAckFailedEvent-11
   * 8. TxCompensateAckFailedEvent-11
   * 9. SagaAbortedEvent-1
   */
  public static List<BaseEvent> middleTxAbortedAndRetryCompensationEvents(String globalTxId, String localTxId_1, String localTxId_2){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).reverseRetries(3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxCompensateAckFailedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxCompensateAckFailedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxAbortedEvent-13
   * 8. SagaAbortedEvent-1
   * 9. TxCompensateAckSucceedEvent-11
   * 10. TxCompensateAckSucceedEvent-12
   */
  public static List<BaseEvent> sagaAbortedEventBeforeTxComponsitedEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxAbortedEvent-11
   * 4. TxStartedEvent-12
   * 5. TxEndedEvent-12
   * 6. TxStartedEvent-13
   * 7. TxEndedEvent-13
   * 8. TxCompensateAckSucceedEvent-12
   * 9. TxCompensateAckSucceedEvent-13
   * 10. SagaAbortedEvent-1
   */
  public static List<BaseEvent> receivedRemainingEventAfterFirstTxAbortedEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
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
   * 9. TxCompensateAckSucceedEvent-11
   * 8. TxCompensateAckSucceedEvent-12
   * 9. TxCompensateAckSucceedEvent-13
   */
  public static List<BaseEvent> sagaAbortedEventAfterAllTxEndedsEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    return sagaEvents;
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
  public static List<BaseEvent> omegaSendSagaTimeoutEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaTimeoutEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
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
  public static List<BaseEvent> sagaActorTriggerTimeoutEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3, int timeout){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).timeout(timeout).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    return sagaEvents;
  }

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
  public static List<BaseEvent> successfulWithTxConcurrentEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
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
  public static List<BaseEvent> successfulWithTxConcurrentCrossEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 4. TxStartedEvent-12
   * 6. TxStartedEvent-13
   * 3. TxEndedEvent-11
   * 5. TxEndedEvent-12
   * 7. TxAbortedEvent-13
   * 8. TxCompensateAckSucceedEvent-11
   * 9. TxCompensateAckSucceedEvent-12
   * 10. SagaAbortedEvent-1
   */
  public static List<BaseEvent> lastTxAbortedEventWithTxConcurrentEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxAbortedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxCompensateAckSucceedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(SagaAbortedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }


  /**
   * 1. SagaStartedEvent-1
   * 2. TxStartedEvent-11
   * 3. TxEndedEvent-11
   * 4. TxStartedEvent-12
   */
  public static List<BaseEvent> successfulFirstHalfEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    return sagaEvents;
  }

  /**
   * 1. TxEndedEvent-12
   * 2. TxStartedEvent-13
   * 3. TxEndedEvent-13
   * 4. SagaEndedEvent-1
   */
  public static List<BaseEvent> successfulSecondHalfEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<BaseEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    sagaEvents.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
    return sagaEvents;
  }

}
