/*
 *
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
 *
 *
 */

package org.apache.servicecomb.saga.alpha.server;

import static org.apache.servicecomb.saga.alpha.core.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxStartedEvent;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicecomb.saga.alpha.core.EventType;
import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.junit.Before;
import org.junit.Test;

import io.grpc.stub.StreamObserver;

public class GrpcTxEventStreamObserverTest {
  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks = new ConcurrentHashMap<>();

  private final Map<StreamObserver<GrpcCompensateCommand>, SimpleImmutableEntry<String, String>> omegaCallbacksReverse = new ConcurrentHashMap<>();

  private final TxConsistentService txConsistentService = mock(TxConsistentService.class);

  private final StreamObserver responseObserver = mock(StreamObserver.class);

  private final GrpcTxEventStreamObserver observer = new GrpcTxEventStreamObserver(omegaCallbacks,
      omegaCallbacksReverse, txConsistentService, responseObserver);

  private final String serviceName = "service a";

  private final String instanceId = "instance a";

  private final GrpcTxEvent startedEvent = eventOf(serviceName, instanceId, TxStartedEvent);

  private final GrpcTxEvent abortedEvent = eventOf(serviceName, instanceId, TxAbortedEvent);

  private final GrpcTxEvent endedEvent = eventOf(serviceName, instanceId, TxEndedEvent);

  @Before
  public void setUp() throws Exception {
    omegaCallbacks.clear();
    omegaCallbacksReverse.clear();
  }

  @Test
  public void updateOmegaCallbacksOnStartedEvent() {
    observer.onNext(startedEvent);

    assertThat(omegaCallbacks.size(), is(1));
    assertThat(omegaCallbacks.getOrDefault(serviceName, null), is(notNullValue()));
    OmegaCallback callback = omegaCallbacks.get(serviceName).getOrDefault(instanceId, null);
    assertThat(callback, is(notNullValue()));
    assertThat(((GrpcOmegaCallback) callback).observer(), is(responseObserver));

    assertThat(omegaCallbacksReverse.size(), is(1));
    assertThat(omegaCallbacksReverse.getOrDefault(responseObserver, null), is(notNullValue()));
    assertThat(omegaCallbacksReverse.get(responseObserver).getKey(), is(serviceName));
    assertThat(omegaCallbacksReverse.get(responseObserver).getValue(), is(instanceId));
  }

  @Test
  public void duplicateEventsOnlyHoldsOneOmegaCallback() {
    observer.onNext(startedEvent);
    observer.onNext(startedEvent);

    assertThat(omegaCallbacks.size(), is(1));
    assertThat(omegaCallbacksReverse.size(), is(1));
  }

  @Test
  public void omegaCallbacksNotChangeOnOtherEvents() {
    observer.onNext(abortedEvent);
    observer.onNext(endedEvent);

    assertThat(omegaCallbacks.isEmpty(), is(true));
  }

  @Test
  public void removeOmegaCallbacksOnComplete() {
    observer.onNext(startedEvent);
    assertThat(omegaCallbacks.getOrDefault(serviceName, new HashMap<>()).isEmpty(), is(false));
    assertThat(omegaCallbacksReverse.size(), is(1));

    observer.onCompleted();
    assertThat(omegaCallbacks.getOrDefault(serviceName, new HashMap<>()).isEmpty(), is(true));
    assertThat(omegaCallbacksReverse.isEmpty(), is(true));
  }

  private GrpcTxEvent eventOf(String serviceName, String instanceId, EventType type) {
    return GrpcTxEvent.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setType(type.name())
        .build();
  }
}