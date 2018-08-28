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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.annotations.TccStart;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TccStartAspectTest {
  private final ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
  private final MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
  private final String globalTxId = UUID.randomUUID().toString();
  private final List<TccStartedEvent> startedEvents = new ArrayList<>();
  private final List<TccEndedEvent> endedEvents = new ArrayList<>();
  private final AlphaResponse response = new AlphaResponse(false);
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
      return null;
    }

    @Override
    public AlphaResponse tccTransactionStart(TccStartedEvent tccStartEvent) {
      startedEvents.add(tccStartEvent);
      return response;
    }

    @Override
    public AlphaResponse tccTransactionStop(TccEndedEvent tccEndEvent) {
      endedEvents.add(tccEndEvent);
      return response;
    }

    @Override
    public AlphaResponse coordinate(CoordinatedEvent coordinatedEvent) {
      return null;
    }
  };


  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);
  private final TccStart tccStart = Mockito.mock(TccStart.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final TccStartAspect aspect = new TccStartAspect(eventService, omegaContext);

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(globalTxId);
    when(joinPoint.getSignature()).thenReturn(methodSignature);

    when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("doNothing"));
    omegaContext.clear();
  }

  @Test
  public void newGlobalTxIdInTccStart() throws Throwable {
    aspect.advise(joinPoint, tccStart);

    assertThat(startedEvents.size(), is(1));
    TccStartedEvent startedEvent = startedEvents.get(0);

    assertThat(startedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(startedEvent.getLocalTxId(), is(globalTxId));

    assertThat(endedEvents.size(), is(1));
    TccEndedEvent endedEvent = endedEvents.get(0);

    assertThat(endedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(endedEvent.getLocalTxId(), is(globalTxId));
    assertThat(endedEvent.getStatus(), is(TransactionStatus.Succeed));


    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }

  @Test
  public void clearContextOnTccStartError() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");

    when(joinPoint.proceed()).thenThrow(oops);

    try {
      aspect.advise(joinPoint, tccStart);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e, is(oops));
    }

    assertThat(startedEvents.size(), is(1));
    TccStartedEvent event = startedEvents.get(0);

    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(globalTxId));
    
    TccEndedEvent endedEvent = endedEvents.get(0);

    assertThat(endedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(endedEvent.getLocalTxId(), is(globalTxId));
    assertThat(endedEvent.getStatus(), is(TransactionStatus.Failed));

    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }

  private String doNothing() {
    return "doNothing";
  }


}
