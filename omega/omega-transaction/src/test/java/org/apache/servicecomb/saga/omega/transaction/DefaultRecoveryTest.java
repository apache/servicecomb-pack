/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.transaction.InvalidTransactionException;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;

public class DefaultRecoveryTest {
  private final List<TxEvent> messages = new ArrayList<>();

  private final String globalTxId = UUID.randomUUID().toString();

  private final String localTxId = UUID.randomUUID().toString();

  private final String parentTxId = UUID.randomUUID().toString();

  private final String newLocalTxId = UUID.randomUUID().toString();

  private final RuntimeException oops = new RuntimeException("oops");

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = mock(IdGenerator.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);

  private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

  private final MethodSignature methodSignature = mock(MethodSignature.class);

  private final Compensable compensable = mock(Compensable.class);

  private final MessageSender sender = e -> {
    messages.add(e);
    return new AlphaResponse(false);
  };

  private final CompensableInterceptor interceptor = new CompensableInterceptor(omegaContext, sender);

  private final RecoveryPolicy recoveryPolicy = new DefaultRecovery();

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
  public void recordEndedEventWhenSuccess() throws Throwable {
    when(joinPoint.proceed()).thenReturn(null);
    recoveryPolicy.apply(joinPoint, compensable, interceptor, omegaContext, parentTxId, 0);

    assertThat(messages.size(), is(2));

    TxEvent startedEvent = messages.get(0);
    assertThat(startedEvent.globalTxId(), is(globalTxId));
    assertThat(startedEvent.localTxId(), is(localTxId));
    assertThat(startedEvent.parentTxId(), is(parentTxId));
    assertThat(startedEvent.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent.retries(), is(0));
    assertThat(startedEvent.retryMethod(), is(""));

    TxEvent endedEvent = messages.get(1);
    assertThat(endedEvent.globalTxId(), is(globalTxId));
    assertThat(endedEvent.localTxId(), is(localTxId));
    assertThat(endedEvent.parentTxId(), is(parentTxId));
    assertThat(endedEvent.type(), is(EventType.TxEndedEvent));
  }

  @Test
  public void recordAbortedEventWhenFailed() throws Throwable {
    when(joinPoint.proceed()).thenThrow(oops);

    try {
      recoveryPolicy.apply(joinPoint, compensable, interceptor, omegaContext, parentTxId, 0);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("oops"));
    }

    assertThat(messages.size(), is(2));

    TxEvent startedEvent = messages.get(0);
    assertThat(startedEvent.globalTxId(), is(globalTxId));
    assertThat(startedEvent.localTxId(), is(localTxId));
    assertThat(startedEvent.parentTxId(), is(parentTxId));
    assertThat(startedEvent.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent.retries(), is(0));
    assertThat(startedEvent.retryMethod(), is(""));

    TxEvent abortedEvent = messages.get(1);
    assertThat(abortedEvent.globalTxId(), is(globalTxId));
    assertThat(abortedEvent.localTxId(), is(localTxId));
    assertThat(abortedEvent.parentTxId(), is(parentTxId));
    assertThat(abortedEvent.type(), is(EventType.TxAbortedEvent));
  }

  @Test
  public void returnImmediatelyWhenReceivedRejectResponse() {
    MessageSender sender = mock(MessageSender.class);
    when(sender.send(any())).thenReturn(new AlphaResponse(true));

    CompensableInterceptor interceptor = new CompensableInterceptor(omegaContext, sender);

    try {
      recoveryPolicy.apply(joinPoint, compensable, interceptor, omegaContext, parentTxId, 0);
      expectFailing(InvalidTransactionException.class);
    } catch (InvalidTransactionException e) {
      assertThat(e.getMessage().contains("Abort sub transaction"), is(true));
    } catch (Throwable throwable) {
      fail("unexpected exception throw: " + throwable);
    }

    verify(sender, times(1)).send(any());
  }

  @Test
  public void recordRetryMethodWhenRetriesIsSet() throws Throwable {
    int retries = new Random().nextInt(Integer.MAX_VALUE - 1) + 1;
    when(compensable.retries()).thenReturn(retries);

    recoveryPolicy.apply(joinPoint, compensable, interceptor, omegaContext, parentTxId, retries);

    TxEvent startedEvent = messages.get(0);

    assertThat(startedEvent.globalTxId(), is(globalTxId));
    assertThat(startedEvent.localTxId(), is(localTxId));
    assertThat(startedEvent.parentTxId(), is(parentTxId));
    assertThat(startedEvent.type(), is(EventType.TxStartedEvent));
    assertThat(startedEvent.retries(), is(retries));
    assertThat(startedEvent.retryMethod(), is(this.getClass().getDeclaredMethod("doNothing").toString()));
  }

  private String doNothing() {
    return "doNothing";
  }
}