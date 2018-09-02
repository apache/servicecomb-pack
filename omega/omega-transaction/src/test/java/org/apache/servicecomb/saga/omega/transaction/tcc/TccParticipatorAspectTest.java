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

package org.apache.servicecomb.saga.omega.transaction.tcc;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TccParticipatorAspectTest {
  private final ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
  private final MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String newLocalTxId = UUID.randomUUID().toString();

  private final List<ParticipatedEvent> participatedEvents = new ArrayList<>();
  private final AlphaResponse response = new AlphaResponse(false);
  private String confirmMethod;
  private String cancelMethod;
  private final TccEventService eventService = new TccEventService() {
    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void close() {

    }

    @Override
    public String target() {
      return null;
    }

    @Override
    public AlphaResponse participate(ParticipatedEvent participateEvent) {
      participatedEvents.add(participateEvent);
      return response;
    }

    @Override
    public AlphaResponse tccTransactionStart(TccStartedEvent tccStartEvent) {
      return null;
    }

    @Override
    public AlphaResponse tccTransactionStop(TccEndedEvent tccEndEvent) {
      return null;
    }

    @Override
    public AlphaResponse coordinate(CoordinatedEvent coordinatedEvent) {
      return null;
    }
  };


  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final Participate participate = mock(Participate.class);
  private final ParametersContext parametersContext = new DefaultParametersContext();

  private final TccParticipatorAspect aspect = new TccParticipatorAspect(eventService, omegaContext, parametersContext);


  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(newLocalTxId);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(joinPoint.getTarget()).thenReturn(this);

    when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("doNothing"));
    when(participate.cancelMethod()).thenReturn("cancelMethod");
    when(participate.confirmMethod()).thenReturn("confirmMethod");

    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);

    confirmMethod = TccParticipatorAspectTest.class.getDeclaredMethod("confirmMethod").toString();
    cancelMethod = TccParticipatorAspectTest.class.getDeclaredMethod("cancelMethod").toString();
  }

  @Test
  public void participateMethodIsCalledSuccessed() throws Throwable {
    aspect.advise(joinPoint, participate);

    assertThat(participatedEvents.size(), is(1));
    ParticipatedEvent participatedEvent = participatedEvents.get(0);

    assertThat(participatedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(participatedEvent.getParentTxId(), is(localTxId));
    assertThat(participatedEvent.getLocalTxId(), is(newLocalTxId));
    assertThat(participatedEvent.getStatus(), is(TransactionStatus.Succeed));
    assertThat(participatedEvent.getCancelMethod(), is(cancelMethod));
    assertThat(participatedEvent.getConfirmMethod(), is(confirmMethod));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void participateMethodIsCalledFailed()  throws Throwable {
    RuntimeException oops = new RuntimeException("oops");

    when(joinPoint.proceed()).thenThrow(oops);

    try {
      aspect.advise(joinPoint, participate);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e, is(oops));
    }

    assertThat(participatedEvents.size(), is(1));
    ParticipatedEvent participatedEvent = participatedEvents.get(0);

    assertThat(participatedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(participatedEvent.getParentTxId(), is(localTxId));
    assertThat(participatedEvent.getLocalTxId(), is(newLocalTxId));
    assertThat(participatedEvent.getStatus(), is(TransactionStatus.Failed));
    assertThat(participatedEvent.getCancelMethod(), is(cancelMethod));
    assertThat(participatedEvent.getConfirmMethod(), is(confirmMethod));


    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  private String doNothing() {
    return "doNothing";
  }

  private String cancelMethod() {
    return "cancelMethod";
  }

  private String confirmMethod() {
    return "confirmMethod";
  }

}
