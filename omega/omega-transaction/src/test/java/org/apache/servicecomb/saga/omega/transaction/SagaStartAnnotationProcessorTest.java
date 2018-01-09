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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.Test;

public class SagaStartAnnotationProcessorTest {

  private final List<TxEvent> messages = new ArrayList<>();

  private final MessageSender sender = messages::add;

  private final String globalTxId = UUID.randomUUID().toString();

  private final String localTxId = UUID.randomUUID().toString();

  private final IdGenerator generator = mock(IdGenerator.class);

  @SuppressWarnings("unchecked")
  private final OmegaContext context = new OmegaContext(generator);

  private final SagaStartAnnotationProcessor sagaStartAnnotationProcessor = new SagaStartAnnotationProcessor(context,
      sender);

  @Test
  public void sendsSagaStartedEvent() {
    when(generator.nextId()).thenReturn(globalTxId, localTxId);

    sagaStartAnnotationProcessor.preIntercept();

    assertThat(context.globalTxId(), is(globalTxId));
    assertThat(context.localTxId(), is(localTxId));
    assertThat(context.parentTxId(), is(globalTxId));

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(globalTxId));
    assertThat(event.parentTxId(), is(nullValue()));
    assertThat(event.compensationMethod().isEmpty(), is(true));
    assertThat(event.type(), is("SagaStartedEvent"));
    assertThat(event.payloads().length, is(0));
  }

  @Test
  public void sendsSagaEndedEvent() {
    context.clear();
    context.setGlobalTxId(globalTxId);
    context.setLocalTxId(localTxId);

    sagaStartAnnotationProcessor.postIntercept();

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(globalTxId));
    assertThat(event.parentTxId(), is(nullValue()));
    assertThat(event.compensationMethod().isEmpty(), is(true));
    assertThat(event.type(), is("SagaEndedEvent"));
    assertThat(event.payloads().length, is(0));
  }
}