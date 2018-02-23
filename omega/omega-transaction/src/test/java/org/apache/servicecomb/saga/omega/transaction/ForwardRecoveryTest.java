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

public class ForwardRecoveryTest {
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

  private final RecoveryPolicy recoveryPolicy = new ForwardRecovery();

  private volatile OmegaException exception;

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
  public void forwardExceptionWhenGlobalTxAborted() {
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
  public void throwExceptionWhenRetryReachesMaximum() throws Throwable {
    when(compensable.retries()).thenReturn(2);
    when(joinPoint.proceed()).thenThrow(oops);

    try {
      recoveryPolicy.apply(joinPoint, compensable, interceptor, omegaContext, parentTxId, 2);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), is("oops"));
    }

    assertThat(messages.size(), is(4));
    assertThat(messages.get(0).type(), is(EventType.TxStartedEvent));
    assertThat(messages.get(1).type(), is(EventType.TxAbortedEvent));
    assertThat(messages.get(2).type(), is(EventType.TxStartedEvent));
    assertThat(messages.get(3).type(), is(EventType.TxAbortedEvent));
  }

  @Test
  public void keepRetryingTillInterrupted() throws Throwable {
    when(compensable.retries()).thenReturn(-1);
    when(compensable.retryDelayInMilliseconds()).thenReturn(1000);
    when(joinPoint.proceed()).thenThrow(oops);

    Thread thread = new Thread(() -> {
      try {
        recoveryPolicy.apply(joinPoint, compensable, interceptor, omegaContext, parentTxId, -1);
        expectFailing(OmegaException.class);
      } catch (OmegaException e) {
        exception = e;
      } catch (Throwable throwable) {
        fail("unexpected exception throw: " + throwable);
      }
    });
    thread.start();

    thread.interrupt();
    thread.join();

    assertThat(exception.getMessage().contains("Failed to handle tx because it is interrupted"), is(true));
  }

  private String doNothing() {
    return "doNothing";
  }
}