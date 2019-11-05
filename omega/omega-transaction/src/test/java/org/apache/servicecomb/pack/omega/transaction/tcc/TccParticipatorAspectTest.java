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

package org.apache.servicecomb.pack.omega.transaction.tcc;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.annotations.Participate;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.*;
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

  private final List<ParticipationStartedEvent> participationStartedEvents = new ArrayList<>();
  private final List<ParticipationEndedEvent> participationEndedEvents = new ArrayList<>();
  private final AlphaResponse response = new AlphaResponse(false);
  private String confirmMethod;
  private String cancelMethod;

  private final String transactionGlobalTxId = UUID.randomUUID().toString();
  private final String transactionLocalTxId = UUID.randomUUID().toString();
  private final TransactionContextProperties transactionContextProperties = mock(TransactionContextProperties.class);

  private final TccMessageSender tccMessageSender = new TccMessageSender() {
    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public ServerMeta onGetServerMeta() {
      return null;
    }

    @Override
    public void close() {

    }

    @Override
    public String target() {
      return null;
    }

    @Override
    public AlphaResponse participationStart(ParticipationStartedEvent participationStartedEvent) {
      participationStartedEvents.add(participationStartedEvent);
      return response;
    }

    @Override
    public AlphaResponse participationEnd(ParticipationEndedEvent participationEndedEvent) {
      participationEndedEvents.add(participationEndedEvent);
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

  private final TccParticipatorAspect aspect = new TccParticipatorAspect(tccMessageSender, omegaContext, parametersContext);


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

    when(transactionContextProperties.getGlobalTxId()).thenReturn(transactionGlobalTxId);
    when(transactionContextProperties.getLocalTxId()).thenReturn(transactionLocalTxId);
  }

  @Test
  public void injectTransactionContextExplicitly() throws Throwable {

    when(joinPoint.getArgs()).thenReturn(new Object[]{transactionContextProperties});

    aspect.advise(joinPoint, participate);

    assertThat(participationStartedEvents.size(), is(1));
    ParticipationStartedEvent participationStartedEvent = participationStartedEvents.get(0);

    assertThat(participationStartedEvent.getGlobalTxId(), is(transactionGlobalTxId));
    assertThat(participationStartedEvent.getParentTxId(), is(transactionLocalTxId));
    assertThat(participationStartedEvent.getLocalTxId(), is(newLocalTxId));

    assertThat(omegaContext.globalTxId(), is(transactionGlobalTxId));
    assertThat(omegaContext.localTxId(), is(transactionLocalTxId));

  }

  @Test
  public void participateMethodIsCalledSuccessed() throws Throwable {
    aspect.advise(joinPoint, participate);

    assertThat(participationStartedEvents.size(), is(1));
    ParticipationStartedEvent participationStartedEvent = participationStartedEvents.get(0);

    assertThat(participationStartedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(participationStartedEvent.getParentTxId(), is(localTxId));
    assertThat(participationStartedEvent.getLocalTxId(), is(newLocalTxId));
//    assertThat(participationStartedEvent.getStatus(), is(TransactionStatus.Succeed));
//    assertThat(participationStartedEvent.getCancelMethod(), is(cancelMethod));
//    assertThat(participationStartedEvent.getConfirmMethod(), is(confirmMethod));

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

    assertThat(participationStartedEvents.size(), is(1));
    ParticipationStartedEvent participationStartedEvent = participationStartedEvents.get(0);

    assertThat(participationStartedEvent.getGlobalTxId(), is(globalTxId));
    assertThat(participationStartedEvent.getParentTxId(), is(localTxId));
    assertThat(participationStartedEvent.getLocalTxId(), is(newLocalTxId));
//    assertThat(participationStartedEvent.getStatus(), is(TransactionStatus.Failed));
//    assertThat(participationStartedEvent.getCancelMethod(), is(cancelMethod));
//    assertThat(participationStartedEvent.getConfirmMethod(), is(confirmMethod));


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
