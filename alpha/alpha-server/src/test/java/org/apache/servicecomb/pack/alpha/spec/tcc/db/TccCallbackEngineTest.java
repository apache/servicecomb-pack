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

package org.apache.servicecomb.pack.alpha.spec.tcc.db;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.grpc.stub.StreamObserver;
import java.util.UUID;

import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.OmegaCallbacksRegistry;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.TccCallbackEngine;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccTxEventService;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.apache.servicecomb.pack.contract.grpc.GrpcServiceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TccApplication.class},
    properties = {
        "alpha.spec.names=tcc-db",
        "alpha.spec.tcc.db.memory-mode=true",
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8092",
        "alpha.compensation.retry.delay=30",
        "spring.profiles.active=tccTest"
    })
public class TccCallbackEngineTest {

  @Autowired
  private TccCallbackEngine tccCallbackEngine;

  @Autowired
  private TccTxEventService tccTxEventService;

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String confirmMethod = "confirm";
  private final String cancelMethod = "cancel";

  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final GrpcServiceConfig serviceConfig = GrpcServiceConfig.newBuilder()
      .setServiceName(serviceName)
      .setInstanceId(instanceId)
      .build();

  private final GrpcServiceConfig serviceConfig2 = GrpcServiceConfig.newBuilder()
      .setServiceName(serviceName)
      .setInstanceId(uniquify("instanceId"))
      .build();

  private ParticipatedEvent participatedEvent;
  private ParticipatedEvent participationStartedEvent;
  private GlobalTxEvent tccEndEvent;

  @Before
  public void init() {
    participationStartedEvent = new ParticipatedEvent(serviceName, instanceId, globalTxId, localTxId,
        parentTxId, confirmMethod, cancelMethod, "");
    participatedEvent = new ParticipatedEvent(serviceName, instanceId, globalTxId, localTxId,
        parentTxId, confirmMethod, cancelMethod, TransactionStatus.Succeed.name());

    tccEndEvent = new GlobalTxEvent(serviceName, instanceId, globalTxId,
        localTxId, parentTxId, TccTxType.ENDED.name(), TransactionStatus.Succeed.name());
  }

  @After
  public void teardown() {
  }

  @Test
  public void sendCoordinateCommandAfterTccEnd() {
    StreamObserver responseObserver = mock(StreamObserver.class);
    OmegaCallbacksRegistry.register(serviceConfig, responseObserver);

    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participatedEvent);

    tccTxEventService.onTccEndedEvent(tccEndEvent);

    verify(responseObserver).onNext(any());
  }

  @Test
  public void sendCoordinateFailedForOmegaDown() throws InterruptedException {
    StreamObserver responseObserver = mock(StreamObserver.class);
    doThrow(IllegalArgumentException.class).when(responseObserver).onNext(any());
    OmegaCallbacksRegistry.register(serviceConfig, responseObserver);

    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participatedEvent);
    boolean result = tccCallbackEngine.execute(tccEndEvent);
    assertThat(result, is(false));

    Thread.sleep(1000);
    verify(responseObserver).onNext(any());

    try {
      OmegaCallbacksRegistry.retrieve(serviceName, instanceId);
    } catch (Exception ex) {
      assertThat(ex.getMessage().startsWith("No such omega callback found for service"), is(true));
    }
  }

  @Test
  public void doRetryCoordinateTillOmegaReceived() throws InterruptedException {
    StreamObserver failedResponseObserver = mock(StreamObserver.class);
    doThrow(IllegalArgumentException.class).when(failedResponseObserver).onNext(any());
    OmegaCallbacksRegistry.register(serviceConfig, failedResponseObserver);

    tccTxEventService.onParticipationStartedEvent(participationStartedEvent);
    tccTxEventService.onParticipationEndedEvent(participatedEvent);
    boolean result = tccCallbackEngine.execute(tccEndEvent);
    assertThat(result, is(false));

    Thread.sleep(1000);

    StreamObserver succeedResponseObserver = mock(StreamObserver.class);
    OmegaCallbacksRegistry.register(serviceConfig2, succeedResponseObserver);

    Thread.sleep(1000);
    verify(failedResponseObserver).onNext(any());
    verify(succeedResponseObserver).onNext(any());
  }
}
