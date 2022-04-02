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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.Optional;

import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEventDBRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.TccApplication;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.TccConfiguration;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEvent;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TccApplication.class, TccConfiguration.class}, properties = {
    "alpha.spec.names=tcc-db",
    "spring.profiles.active=tccTest"
})
public class TccTxEventServiceTransactionTest {

  @Autowired
  private TccTxEventService tccTxEventService;

  @MockBean
  private TccTxEventDBRepository tccTxEventDBRepository;

  @Autowired
  private ParticipatedEventRepository participatedEventRepository;

  @Autowired
  private GlobalTxEventRepository globalTxEventRepository;

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String confirmMethod = "confirm";
  private final String cancelMethod = "cancel";
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private GlobalTxEvent tccStartEvent;
  private ParticipatedEvent participatedEvent;
  private GlobalTxEvent tccEndEvent;
  private TccTxEvent coordinateEvent;

  @Before
  public void setup() {
    tccStartEvent = new GlobalTxEvent(serviceName, instanceId, globalTxId,
        localTxId, parentTxId, TccTxType.STARTED.name(), TransactionStatus.Succeed.name());

    participatedEvent = new ParticipatedEvent(serviceName, instanceId, globalTxId, localTxId,
        parentTxId, confirmMethod, cancelMethod, TransactionStatus.Succeed.name());

    tccEndEvent = new GlobalTxEvent(serviceName, instanceId, globalTxId,
        localTxId, parentTxId, TccTxType.ENDED.name(), TransactionStatus.Succeed.name());

    coordinateEvent = new TccTxEvent(serviceName, instanceId, globalTxId,
        localTxId, parentTxId, TccTxType.COORDINATED.name(), TransactionStatus.Succeed.name());
  }

  @After
  public void teardown() {
  }

  @Test
  public void rollbackAfterSaveTccTxEventDbFailure() {
    doThrow(NullPointerException.class).when(tccTxEventDBRepository).save((TccTxEvent) any());

    tccTxEventService.onTccStartedEvent(tccStartEvent);
    Optional<List<GlobalTxEvent>> startEvents = globalTxEventRepository.findByGlobalTxId(globalTxId);
    assertThat(startEvents.get().isEmpty(), is(true));

    tccTxEventService.onParticipationStartedEvent(participatedEvent);
    Optional<List<ParticipatedEvent>> participates = participatedEventRepository.findByGlobalTxId(globalTxId);
    assertThat(participates.get().isEmpty(), is(true));

    tccTxEventService.onTccEndedEvent(tccEndEvent);
    Optional<List<GlobalTxEvent>> endEvents = globalTxEventRepository.findByGlobalTxId(globalTxId);
    assertThat(endEvents.get().isEmpty(), is(true));

    participatedEventRepository.save(participatedEvent);
    tccTxEventService.onCoordinatedEvent(coordinateEvent);
    participates = participatedEventRepository.findByGlobalTxId(globalTxId);
    assertThat(participates.get().isEmpty(), is(false));
  }
}
