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

package org.apache.servicecomb.pack.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.servicecomb.pack.common.EventType;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.omega.context.AlphaMetas;
import org.apache.servicecomb.pack.omega.context.IdGenerator;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.UniqueIdGenerator;
import org.junit.Before;
import org.junit.Test;

public class CompensationMessageHandlerTest {

  private final List<TxEvent> events = new ArrayList<>();
  private final SagaMessageSender sender = new SagaMessageSender() {
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
      return "UNKNOWN"; }

    @Override
    public AlphaResponse send(TxEvent event) {
      events.add(event);
      return new AlphaResponse(false);
    }
  };

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");

  private final String compensationMethod = getClass().getCanonicalName();
  private final String payload = uniquify("blah");

  @Before
  public void setUp() {
    events.clear();
  }

  @Test
  public void sendsCompensatedEventOnCompensationCompleted() {
    final CallbackContext context = mock(CallbackContext.class);
    final CompensationMessageHandler handler = new CompensationMessageHandler(sender, context);
    IdGenerator<String> idGenerator = new UniqueIdGenerator();
    OmegaContext omegaContext = new OmegaContext(idGenerator,AlphaMetas.builder().akkaEnabled(false).build());
    when(context.getOmegaContext()).thenReturn(omegaContext);
    handler.onReceive(globalTxId, localTxId, parentTxId, compensationMethod, payload);
    assertThat(events.size(), is(1));
    TxEvent event = events.get(0);
    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxCompensatedEvent));
    assertThat(event.compensationMethod(), is(getClass().getCanonicalName()));
    assertThat(event.payloads().length, is(0));
    verify(context).apply(globalTxId, localTxId, parentTxId, compensationMethod, payload);
  }

  @Test
  public void sendsCompensateAckSucceedEventOnCompensationCompletedWithFSM() throws NoSuchMethodException {
    IdGenerator<String> idGenerator = new UniqueIdGenerator();
    OmegaContext omegaContext = new OmegaContext(idGenerator,AlphaMetas.builder().akkaEnabled(true).build());
    CallbackContext context = new CallbackContext(omegaContext, sender);
    Method mockMethod = this.getClass().getMethod("mockCompensationSucceedMethod",String.class);
    context.addCallbackContext(compensationMethod, mockMethod, this);
    CompensationMessageHandler handler = new CompensationMessageHandler(sender, context);
    handler.onReceive(globalTxId, localTxId, parentTxId, compensationMethod, payload);
    assertThat(events.size(), is(1));
    TxEvent event = events.get(0);
    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxCompensateAckSucceedEvent));
    assertThat(event.compensationMethod(), is(getClass().getCanonicalName()));
    assertThat(event.payloads().length, is(0));
  }

  @Test
  public void sendsCompensateAckFailedEventOnCompensationFailedWithFSM() throws NoSuchMethodException {
    IdGenerator<String> idGenerator = new UniqueIdGenerator();
    OmegaContext omegaContext = new OmegaContext(idGenerator,AlphaMetas.builder().akkaEnabled(true).build());
    CallbackContext context = new CallbackContext(omegaContext, sender);
    Method mockMethod = this.getClass().getMethod("mockCompensationFailedMethod",String.class);
    context.addCallbackContext(compensationMethod, mockMethod, this);
    CompensationMessageHandler handler = new CompensationMessageHandler(sender, context);
    handler.onReceive(globalTxId, localTxId, parentTxId, compensationMethod, payload);
    assertThat(events.size(), is(1));
    TxEvent event = events.get(0);
    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxCompensateAckFailedEvent));
    assertThat(event.compensationMethod(), is(getClass().getCanonicalName()));
    assertThat(event.payloads().length, greaterThan(0));
  }

  public void mockCompensationSucceedMethod(String payloads){
    // mock compensation method
  }

  public void mockCompensationFailedMethod(String payloads){
    // mock compensation method
    throw new RuntimeException("mock compensation failed");
  }
}
