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

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CompensableInterceptorTest {

  private final List<TxEvent> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();

  private final MessageSender sender = e -> {
    messages.add(e);
    return new AlphaResponse(false);
  };

  private final String message = uniquify("message");

  private final String retryMethod = uniquify("retryMethod");
  private final String compensationMethod = getClass().getCanonicalName();

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = Mockito.mock(IdGenerator.class);
  private final OmegaContext context = new OmegaContext(idGenerator);
  private final CompensableInterceptor interceptor = new CompensableInterceptor(context, sender);

  @Before
  public void setUp() throws Exception {
    context.setGlobalTxId(globalTxId);
    context.setLocalTxId(localTxId);
  }

  @Test
  public void sendsTxStartedEventBefore() throws Exception {
    int retries = new Random().nextInt();
    interceptor.preIntercept(parentTxId, compensationMethod, 0, retryMethod, retries, message);

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.retries(), is(retries));
    assertThat(event.retryMethod(), is(retryMethod));
    assertThat(event.type(), is(EventType.TxStartedEvent));
    assertThat(event.compensationMethod(), is(compensationMethod));
    assertThat(asList(event.payloads()), contains(message));
  }

  @Test
  public void sendsTxEndedEventAfter() throws Exception {
    interceptor.postIntercept(parentTxId, compensationMethod);

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxEndedEvent));
    assertThat(event.compensationMethod(), is(compensationMethod));
    assertThat(event.payloads().length, is(0));
  }

  @Test
  public void sendsTxAbortedEventOnError() throws Exception {
    interceptor.onError(parentTxId, compensationMethod, new RuntimeException("oops"));

    TxEvent event = messages.get(0);

    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxAbortedEvent));
    assertThat(event.compensationMethod(), is(compensationMethod));
  }
}
