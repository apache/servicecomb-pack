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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TransactionAspectTest {
  private final List<TxEvent> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();

  private final String newLocalTxId = UUID.randomUUID().toString();

  private final MessageSender sender = messages::add;
  private final ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
  private final MethodSignature methodSignature = Mockito.mock(MethodSignature.class);

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);
  private final Compensable compensable = Mockito.mock(Compensable.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final TransactionAspect aspect = new TransactionAspect(sender, omegaContext);

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(newLocalTxId);
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(joinPoint.getTarget()).thenReturn(this);

    when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("doNothing"));
    when(compensable.compensationMethod()).thenReturn("doNothing");

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
  public void sendsAbortEventOnTimeout() throws Throwable {
    CountDownLatch latch = new CountDownLatch(1);
    when(compensable.timeout()).thenReturn(100);
    when(joinPoint.proceed()).thenAnswer(invocationOnMock -> {
      latch.await();
      assertThat(omegaContext.localTxId(), is(newLocalTxId));
      return null;
    });

    executor.execute(() -> {
      try {
        // need to setup the thread local for it
        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(localTxId);

        aspect.advise(joinPoint, compensable);
      } catch (Throwable throwable) {
        fail(throwable.getMessage());
      }
    });

    await().atMost(1, SECONDS).until(() -> messages.size() == 2);

    TxEvent event = messages.get(1);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(newLocalTxId));
    assertThat(event.parentTxId(), is(localTxId));
    assertThat(event.type(), is(EventType.TxAbortedEvent));

    latch.countDown();

    await().atMost(1, SECONDS).until(() -> localTxId.equals(omegaContext.localTxId()));

    // no redundant ended message received
    assertThat(messages.size(), is(2));
  }

  private String doNothing() {
    return "doNothing";
  }
}
