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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.transaction.TransactionalException;
import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.Before;
import org.junit.Test;

public class SagaStartAnnotationProcessorTest {

  private final List<TxEvent> messages = new ArrayList<>();

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

  private final String globalTxId = UUID.randomUUID().toString();

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> generator = mock(IdGenerator.class);
  private final OmegaContext context = new OmegaContext(generator);
  private final OmegaException exception = new OmegaException("exception",
      new RuntimeException("runtime exception"));

  private final SagaStartAnnotationProcessor sagaStartAnnotationProcessor = new SagaStartAnnotationProcessor(
      context,
      sender);

  @Before
  public void setUp() throws Exception {
    context.setGlobalTxId(globalTxId);
    context.setLocalTxId(globalTxId);
  }

  @Test
  public void sendsSagaStartedEvent() {
    sagaStartAnnotationProcessor.preIntercept(0);

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(globalTxId));
    assertThat(event.parentTxId(), is(nullValue()));
    assertThat(event.compensationMethod().isEmpty(), is(true));
    assertThat(event.type(), is(EventType.SagaStartedEvent));
    assertThat(event.payloads().length, is(0));
  }

  @Test
  public void sendsSagaEndedEvent() {
    sagaStartAnnotationProcessor.postIntercept(null);

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(globalTxId));
    assertThat(event.parentTxId(), is(nullValue()));
    assertThat(event.compensationMethod().isEmpty(), is(true));
    assertThat(event.type(), is(EventType.SagaEndedEvent));
    assertThat(event.payloads().length, is(0));
  }

  @Test
  public void transformInterceptedException() {
    MessageSender sender = mock(MessageSender.class);
    SagaStartAnnotationProcessor sagaStartAnnotationProcessor = new SagaStartAnnotationProcessor(
        context, sender);

    doThrow(exception).when(sender).send(any(TxEvent.class));

    try {
      sagaStartAnnotationProcessor.preIntercept(0);
      expectFailing(TransactionalException.class);
    } catch (TransactionalException e) {
      assertThat(e.getMessage(), is("exception"));
      assertThat(e.getCause(), instanceOf(RuntimeException.class));
      assertThat(e.getCause().getMessage(), is("runtime exception"));
    }
  }
}
