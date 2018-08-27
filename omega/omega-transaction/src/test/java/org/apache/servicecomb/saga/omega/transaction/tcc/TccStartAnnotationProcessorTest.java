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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.transaction.TransactionalException;

import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TccStartAnnotationProcessorTest {
  private final String globalTxId = UUID.randomUUID().toString();
  private final AlphaResponse response = new AlphaResponse(false);
  private final AlphaResponse abortResponse = new AlphaResponse(true);
  private final List<TccStartedEvent> startedEvents = new ArrayList<>();
  private final List<TccEndedEvent> endedEvents = new ArrayList<>();
  private boolean throwException = false;

  private final IdGenerator<String> generator = mock(IdGenerator.class);
  private final OmegaContext context = new OmegaContext(generator);
  private final OmegaException exception = new OmegaException("exception", new RuntimeException("runtime exception"));
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
      if (throwException) {
        throw exception;
      }
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
  private final TccStartAnnotationProcessor tccStartAnnotationProcessor = new TccStartAnnotationProcessor(context,
      eventService);

  @Before
  public void setUp() throws Exception {
    context.setGlobalTxId(globalTxId);
    context.setLocalTxId(globalTxId);
  }

  @Test
  public void testSendTccStartEvent() {
    AlphaResponse result = tccStartAnnotationProcessor
        .preIntercept(null, "TccStartMethod", 0);

    TccStartedEvent event = startedEvents.get(0);

    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(globalTxId));
    assertThat(result, is(response));

  }

  @Test
  public void testSendTccStartEventFailed() {
    throwException = true;
    try {
      tccStartAnnotationProcessor
          .preIntercept(null, "TccStartMethod", 0);
      expectFailing(TransactionalException.class);
    } catch (TransactionalException e) {
      Assert.assertThat(e.getMessage(), Is.is("exception"));
      Assert.assertThat(e.getCause(), instanceOf(RuntimeException.class));
      Assert.assertThat(e.getCause().getMessage(), Is.is("runtime exception"));
    }

  }

  @Test
  public void testSendTccEndEventWithoutError() {
    tccStartAnnotationProcessor.postIntercept(null, "TccStartMethod");

    TccEndedEvent event = endedEvents.get(0);

    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(globalTxId));
    assertThat(event.getStatus(), is(TransactionStatus.Succeed));

  }

  @Test
  public void testSendTccEndEventWithError() {
    tccStartAnnotationProcessor.onError(null, "TccStartMethod", null);
    TccEndedEvent event = endedEvents.get(0);
    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(globalTxId));
    assertThat(event.getStatus(), is(TransactionStatus.Failed));
  }
}
