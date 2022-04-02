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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.service;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.grpc.stub.StreamObserver;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.TccApplication;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.OmegaCallbacksRegistry;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEvent;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.apache.servicecomb.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TccApplication.class}, properties = {
    "alpha.spec.names=tcc-db",
    "alpha.spec.tcc.db.memory-mode=false",
    "spring.jpa.show-sql=true",
    "spring.profiles.active=tccTest"
})
public class TccTxEventServiceTest {

  @Autowired
  private TccTxEventService tccTxEventService;

  @Autowired
  private TccTxEventRepository tccTxEventRepository;

  @Autowired
  private GlobalTxEventRepository globalTxEventRepository;

  @Autowired
  private ParticipatedEventRepository participatedEventRepository;

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String confirmMethod = "confirm";
  private final String cancelMethod = "cancel";
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final GrpcServiceConfig serviceConfig = GrpcServiceConfig.newBuilder()
      .setServiceName(serviceName)
      .setInstanceId(instanceId)
      .build();

  private GlobalTxEvent tccStartEvent;
  private ParticipatedEvent participationStartedEvent;
  private ParticipatedEvent participationEndedEvent;
  private GlobalTxEvent tccEndEvent;
  private TccTxEvent coordinateEvent;

  @Before
  public void setup() {
    tccStartEvent = newGlobalTxEvent(TccTxType.STARTED, globalTxId, TransactionStatus.Succeed);
    participationStartedEvent = newParticipationStartedEvent(globalTxId);
    participationEndedEvent = newParticipationEndedEvent(globalTxId, TransactionStatus.Succeed);
    tccEndEvent = newGlobalTxEvent(TccTxType.ENDED, globalTxId, TransactionStatus.Succeed);
    coordinateEvent = newTccTxEvent(TccTxType.COORDINATED, globalTxId, TransactionStatus.Succeed);
  }

  @After
  public void teardown() {
  }

  @Test
  public void onlyCoordinateParticipatedEventOnce() {
    StreamObserver<GrpcTccCoordinateCommand> observer = mock(StreamObserver.class);
    OmegaCallbacksRegistry.register(serviceConfig, observer);

    tccTxEventService.onTccStartedEvent(tccStartEvent);
    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participationEndedEvent);
    tccTxEventService.onTccEndedEvent(tccEndEvent);
    tccTxEventService.onCoordinatedEvent(coordinateEvent);

    verify(observer).onNext(any());

    // if end command was send by twice, coordinate should only be executed once.
    tccTxEventService.onTccEndedEvent(tccEndEvent);
    verify(observer).onNext(any());
  }

  @Test
  public void handleTimeoutGlobalTraction() throws InterruptedException {
    StreamObserver<GrpcTccCoordinateCommand> observer = mock(StreamObserver.class);
    OmegaCallbacksRegistry.register(serviceConfig, observer);

    tccTxEventService.onTccStartedEvent(tccStartEvent);
    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participationEndedEvent);

    Thread.sleep(3000l);
    Date deadLine = new Date(System.currentTimeMillis() - SECONDS.toMillis(2));
    tccTxEventService.handleTimeoutTx(deadLine, 1);

    // global tx has timeout, so participated event will be coordinated through cancel.
    Optional<GlobalTxEvent> timeoutEvent = globalTxEventRepository.findByUniqueKey(globalTxId, localTxId, TccTxType.END_TIMEOUT.name());
    assertThat(timeoutEvent.isPresent(), is(true));
    assertThat(timeoutEvent.get().getStatus(), is(TransactionStatus.Failed.name()));
    assertThat(timeoutEvent.get().getTxType(), is(TccTxType.END_TIMEOUT.name()));
    assertThat(timeoutEvent.get().getGlobalTxId(), is(globalTxId));
    assertThat(timeoutEvent.get().getLocalTxId(), is(localTxId));
    assertThat(timeoutEvent.get().getParentTxId(), is(parentTxId));
    assertThat(timeoutEvent.get().getServiceName(), is(serviceName));
    verify(observer).onNext(any());

    Optional<List<TccTxEvent>> events = tccTxEventRepository.findByGlobalTxId(globalTxId);
    assertThat(events.get().size(), is(4));
  }

  @Test
  public void clearUpCompletedTxFromGlobalTxTable() {
    StreamObserver<GrpcTccCoordinateCommand> observer = mock(StreamObserver.class);
    OmegaCallbacksRegistry.register(serviceConfig, observer);

    tccTxEventService.onTccStartedEvent(tccStartEvent);
    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participationEndedEvent);
    tccTxEventService.onTccEndedEvent(tccEndEvent);
    tccTxEventService.onCoordinatedEvent(coordinateEvent);

    tccTxEventService.clearCompletedGlobalTx(1);

    assertThat(participatedEventRepository.findByGlobalTxId(globalTxId).get().isEmpty(), is(true));
    assertThat(globalTxEventRepository.findByGlobalTxId(globalTxId).get().isEmpty(), is(true));

    Optional<List<TccTxEvent>> events = tccTxEventRepository.findByGlobalTxId(globalTxId);
    assertThat(events.get().size(), is(5));
  }

  @Test
  public void clearUpCompletedTxFromGlobalTxTableMoreThanOne() {
    StreamObserver<GrpcTccCoordinateCommand> observer = mock(StreamObserver.class);
    OmegaCallbacksRegistry.register(serviceConfig, observer);

    // one global tx
    tccTxEventService.onTccStartedEvent(tccStartEvent);
    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participationEndedEvent);
    tccTxEventService.onTccEndedEvent(tccEndEvent);
    tccTxEventService.onCoordinatedEvent(coordinateEvent);

    // another global tx
    String globalTxId_2 = uniquify("globalTxId");
    tccTxEventService.onTccStartedEvent(newGlobalTxEvent(TccTxType.STARTED, globalTxId_2, TransactionStatus.Succeed));
    tccTxEventService.onParticipationStartedEvent(newParticipationStartedEvent(globalTxId_2));
    tccTxEventService.onParticipationEndedEvent(newParticipationEndedEvent(globalTxId_2, TransactionStatus.Succeed));
    tccTxEventService.onTccEndedEvent(newGlobalTxEvent(TccTxType.ENDED, globalTxId_2, TransactionStatus.Succeed));
    tccTxEventService.onCoordinatedEvent(newTccTxEvent(TccTxType.COORDINATED, globalTxId_2, TransactionStatus.Succeed));

    tccTxEventService.clearCompletedGlobalTx(2);

    assertThat(participatedEventRepository.findByGlobalTxId(globalTxId).get().isEmpty(), is(true));
    assertThat(globalTxEventRepository.findByGlobalTxId(globalTxId).get().isEmpty(), is(true));

    Optional<List<TccTxEvent>> events = tccTxEventRepository.findByGlobalTxId(globalTxId);
    assertThat(events.get().size(), is(5));

    events = tccTxEventRepository.findByGlobalTxId(globalTxId_2);
    assertThat(events.get().size(), is(5));

  }

  private ParticipatedEvent newParticipationStartedEvent(String globalTxId) {
    return new ParticipatedEvent(serviceName, instanceId, globalTxId, localTxId,
        parentTxId, confirmMethod, cancelMethod, "");
  }

  private ParticipatedEvent newParticipationEndedEvent(String globalTxId, TransactionStatus transactionStatus) {
    return new ParticipatedEvent(serviceName, instanceId, globalTxId, localTxId,
        parentTxId, confirmMethod, cancelMethod, transactionStatus.name());
  }

  private GlobalTxEvent newGlobalTxEvent(TccTxType tccTxType, String globalTxId, TransactionStatus transactionStatus) {
    return new GlobalTxEvent(serviceName, instanceId, globalTxId,
        localTxId, parentTxId, tccTxType.name(), transactionStatus.name());
  }

  private TccTxEvent newTccTxEvent(TccTxType tccTxType, String globalTxId, TransactionStatus transactionStatus) {
    return new TccTxEvent(serviceName, instanceId, globalTxId,
        localTxId, parentTxId, tccTxType.name(), transactionStatus.name());
  }
}
