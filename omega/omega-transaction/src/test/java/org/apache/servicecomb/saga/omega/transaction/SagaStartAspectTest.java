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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class SagaStartAspectTest {
  private final List<TxEvent> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();

  private final MessageSender sender = new MessageSender() {
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
      return "UNKNOWN";
    }

    @Override
    public AlphaResponse send(TxEvent event) {
      messages.add(event);
      return new AlphaResponse(false);
    }
  };
  private final ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
  private final MethodSignature methodSignature = Mockito.mock(MethodSignature.class);

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);
  private final SagaStart sagaStart = Mockito.mock(SagaStart.class);

  private final OmegaContext omegaContext = new OmegaContext(idGenerator);
  private final SagaStartAspect aspect = new SagaStartAspect(sender, omegaContext);

  @Before
  public void setUp() throws Exception {
    when(idGenerator.nextId()).thenReturn(globalTxId);
    when(joinPoint.getSignature()).thenReturn(methodSignature);

    when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("doNothing"));
    omegaContext.clear();
  }

  @Test
  public void newGlobalTxIdInSagaStart() throws Throwable {
    aspect.advise(joinPoint, sagaStart);

    TxEvent startedEvent = messages.get(0);

    assertThat(startedEvent.globalTxId(), is(globalTxId));
    assertThat(startedEvent.localTxId(), is(globalTxId));
    assertThat(startedEvent.parentTxId(), is(nullValue()));
    assertThat(startedEvent.type(), is(EventType.SagaStartedEvent));

    TxEvent endedEvent = messages.get(1);

    assertThat(endedEvent.globalTxId(), is(globalTxId));
    assertThat(endedEvent.localTxId(), is(globalTxId));
    assertThat(endedEvent.parentTxId(), is(nullValue()));
    assertThat(endedEvent.type(), is(EventType.SagaEndedEvent));

    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }

  @Test
  public void clearContextOnSagaStartError() throws Throwable {
    RuntimeException oops = new RuntimeException("oops");

    when(joinPoint.proceed()).thenThrow(oops);

    try {
      aspect.advise(joinPoint, sagaStart);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException e) {
      assertThat(e, is(oops));
    }

    assertThat(messages.size(), is(1));
    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(globalTxId));
    assertThat(event.parentTxId(), is(nullValue()));
    assertThat(event.type(), is(EventType.SagaStartedEvent));

    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }

  private String doNothing() {
    return "doNothing";
  }
}
