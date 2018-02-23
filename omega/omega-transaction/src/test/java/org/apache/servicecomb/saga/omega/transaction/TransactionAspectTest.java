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

package org.apache.servicecomb.saga.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;

public class TransactionAspectTest {
  private final List<TxEvent> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String newLocalTxId = UUID.randomUUID().toString();

  private final MessageSender sender = e -> {
    messages.add(e);
    return new AlphaResponse(false);
  };
  private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
  private final MethodSignature methodSignature = mock(MethodSignature.class);

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = mock(IdGenerator.class);
  private final Compensable compensable = mock(Compensable.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final TransactionAspect aspect = new TransactionAspect(sender, omegaContext);

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(newLocalTxId);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(joinPoint.getTarget()).thenReturn(this);

    when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("doNothing"));
    when(compensable.compensationMethod()).thenReturn("doNothing");
    when(compensable.retries()).thenReturn(0);

    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);
  }

  @Test
  public void newLocalTxIdInCompensable() throws Throwable {
    aspect.advise(joinPoint, compensable);

    TxEvent startedEvent = messages.get(0);

    assertThat(startedEvent.globalTxId(), is(globalTxId));
    assertThat(startedEvent.localTxId(), is(newLocalTxId));
    assertThat(startedEvent.parentTxId(), is(localTxId));
    assertThat(startedEvent.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent.retries(), is(0));
    assertThat(startedEvent.retryMethod().isEmpty(), is(true));

    TxEvent endedEvent = messages.get(1);

    assertThat(endedEvent.globalTxId(), is(globalTxId));
    assertThat(endedEvent.localTxId(), is(newLocalTxId));
    assertThat(endedEvent.parentTxId(), is(localTxId));
    assertThat(endedEvent.type(), is(EventType.TxEndedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void restoreContextOnCompensableError() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");

    when(joinPoint.proceed()).thenThrow(oops);

    try {
      aspect.advise(joinPoint, compensable);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e, is(oops));
    }

    TxEvent event = messages.get(1);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(newLocalTxId));
    assertThat(event.parentTxId(), is(localTxId));
    assertThat(event.type(), is(EventType.TxAbortedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void retryReachesMaximumAndForwardException() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");
    when(joinPoint.proceed()).thenThrow(oops);
    when(compensable.retries()).thenReturn(3);

    try {
      aspect.advise(joinPoint, compensable);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("oops"));
    }

    assertThat(messages.size(), is(6));

    TxEvent startedEvent1 = messages.get(0);
    assertThat(startedEvent1.globalTxId(), is(globalTxId));
    assertThat(startedEvent1.localTxId(), is(newLocalTxId));
    assertThat(startedEvent1.parentTxId(), is(localTxId));
    assertThat(startedEvent1.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent1.retries(), is(3));
    assertThat(startedEvent1.retryMethod(), is(this.getClass().getDeclaredMethod("doNothing").toString()));

    assertThat(messages.get(1).type(), is(EventType.TxAbortedEvent));

    TxEvent startedEvent2 = messages.get(2);
    assertThat(startedEvent2.localTxId(), is(newLocalTxId));
    assertThat(startedEvent2.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent2.retries(), is(2));

    assertThat(messages.get(3).type(), is(EventType.TxAbortedEvent));

    TxEvent startedEvent3 = messages.get(4);
    assertThat(startedEvent3.localTxId(), is(newLocalTxId));
    assertThat(startedEvent3.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent3.retries(), is(1));

    assertThat(messages.get(5).type(), is(EventType.TxAbortedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void keepRetryingTillSuccess() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");
    when(joinPoint.proceed()).thenThrow(oops).thenThrow(oops).thenReturn(null);
    when(compensable.retries()).thenReturn(-1);

    aspect.advise(joinPoint, compensable);

    assertThat(messages.size(), is(6));

    TxEvent startedEvent1 = messages.get(0);
    assertThat(startedEvent1.globalTxId(), is(globalTxId));
    assertThat(startedEvent1.localTxId(), is(newLocalTxId));
    assertThat(startedEvent1.parentTxId(), is(localTxId));
    assertThat(startedEvent1.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent1.retries(), is(-1));
    assertThat(startedEvent1.retryMethod(), is(this.getClass().getDeclaredMethod("doNothing").toString()));

    assertThat(messages.get(1).type(), is(EventType.TxAbortedEvent));

    TxEvent startedEvent2 = messages.get(2);
    assertThat(startedEvent2.localTxId(), is(newLocalTxId));
    assertThat(startedEvent2.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent2.retries(), is(-1));

    assertThat(messages.get(3).type(), is(EventType.TxAbortedEvent));

    TxEvent startedEvent3 = messages.get(4);
    assertThat(startedEvent3.localTxId(), is(newLocalTxId));
    assertThat(startedEvent3.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent3.retries(), is(-1));

    assertThat(messages.get(5).type(), is(EventType.TxEndedEvent));

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  private String doNothing() {
    return "doNothing";
  }
}
