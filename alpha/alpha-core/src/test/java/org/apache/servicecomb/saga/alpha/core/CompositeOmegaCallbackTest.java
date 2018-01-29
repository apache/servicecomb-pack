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

package org.apache.servicecomb.saga.alpha.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicecomb.saga.common.EventType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CompositeOmegaCallbackTest {

  private final OmegaCallback callback1One = Mockito.mock(OmegaCallback.class);
  private final OmegaCallback callback1Two = Mockito.mock(OmegaCallback.class);

  private final OmegaCallback callback2One = Mockito.mock(OmegaCallback.class);
  private final OmegaCallback callback2Two = Mockito.mock(OmegaCallback.class);

  private final String serviceName1 = uniquify("serviceName1");
  private final String instanceId1One = uniquify("instanceId1One");
  private final String instanceId1Two = uniquify("instanceId1Two");

  private final String serviceName2 = uniquify("serviceName2");
  private final String instanceId2One = uniquify("instanceId2One");
  private final String instanceId2Two = uniquify("instanceId2Two");

  private final Map<String, Map<String, OmegaCallback>> callbacks = new ConcurrentHashMap<>();
  private final CompositeOmegaCallback compositeOmegaCallback = new CompositeOmegaCallback(callbacks);

  @Before
  public void setUp() throws Exception {
    callbacks.put(serviceName1, new ConcurrentHashMap<>());
    callbacks.get(serviceName1).put(instanceId1One, callback1One);
    callbacks.get(serviceName1).put(instanceId1Two, callback1Two);

    callbacks.put(serviceName2, new ConcurrentHashMap<>());
    callbacks.get(serviceName2).put(instanceId2One, callback2One);
    callbacks.get(serviceName2).put(instanceId2Two, callback2Two);
  }

  @Test
  public void compensateCorrespondingOmegaInstanceOnly() throws Exception {
    TxEvent event = eventOf(serviceName2, instanceId2One, TxStartedEvent);

    compositeOmegaCallback.compensate(event);

    verify(callback1One, never()).compensate(event);
    verify(callback1Two, never()).compensate(event);
    verify(callback2One).compensate(event);
    verify(callback2Two, never()).compensate(event);

    assertThat(callbacks.get(serviceName1).values(), containsInAnyOrder(callback1One, callback1Two));
    assertThat(callbacks.get(serviceName2).values(), containsInAnyOrder(callback2One, callback2Two));
  }

  @Test
  public void compensateOtherOmegaInstance_IfTheRequestedIsUnreachable() throws Exception {
    callbacks.get(serviceName2).remove(instanceId2One);
    TxEvent event = eventOf(serviceName2, instanceId2One, TxStartedEvent);

    compositeOmegaCallback.compensate(event);

    verify(callback1One, never()).compensate(event);
    verify(callback1Two, never()).compensate(event);
    verify(callback2One, never()).compensate(event);
    verify(callback2Two).compensate(event);

    assertThat(callbacks.get(serviceName1).values(), containsInAnyOrder(callback1One, callback1Two));
    assertThat(callbacks.get(serviceName2).values(), containsInAnyOrder(callback2Two));
  }

  @Test
  public void blowsUpIfNoSuchServiceIsReachable() throws Exception {
    callbacks.get(serviceName2).clear();
    TxEvent event = eventOf(serviceName2, instanceId2One, TxStartedEvent);

    try {
      compositeOmegaCallback.compensate(event);
      expectFailing(AlphaException.class);
    } catch (AlphaException e) {
      assertThat(e.getMessage(), is("No such omega callback found for service " + serviceName2));
    }

    verify(callback1One, never()).compensate(event);
    verify(callback1Two, never()).compensate(event);
    verify(callback2One, never()).compensate(event);
    verify(callback2Two, never()).compensate(event);

    assertThat(callbacks.get(serviceName1).values(), containsInAnyOrder(callback1One, callback1Two));
    assertThat(callbacks.get(serviceName2).isEmpty(), is(true));
  }

  @Test
  public void blowsUpIfNoSuchServiceFound() throws Exception {
    callbacks.remove(serviceName2);
    TxEvent event = eventOf(serviceName2, instanceId2One, TxStartedEvent);

    try {
      compositeOmegaCallback.compensate(event);
      expectFailing(AlphaException.class);
    } catch (AlphaException e) {
      assertThat(e.getMessage(), is("No such omega callback found for service " + serviceName2));
    }

    verify(callback1One, never()).compensate(event);
    verify(callback1Two, never()).compensate(event);
    verify(callback2One, never()).compensate(event);
    verify(callback2Two, never()).compensate(event);

    assertThat(callbacks.get(serviceName1).values(), containsInAnyOrder(callback1One, callback1Two));
    assertThat(callbacks.containsKey(serviceName2), is(false));
  }

  @Test
  public void removeCallbackOnException() throws Exception {
    doThrow(RuntimeException.class).when(callback1Two).compensate(any(TxEvent.class));
    TxEvent event = eventOf(serviceName1, instanceId1Two, TxStartedEvent);

    try {
      compositeOmegaCallback.compensate(event);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException ignored) {
    }

    assertThat(callbacks.get(serviceName1).values(), containsInAnyOrder(callback1One));
    assertThat(callbacks.get(serviceName2).values(), containsInAnyOrder(callback2One, callback2Two));
  }

  private TxEvent eventOf(String serviceName, String instanceId, EventType eventType) {
    return new TxEvent(
        serviceName,
        instanceId,
        uniquify("globalTxId"),
        uniquify("localTxId"),
        UUID.randomUUID().toString(),
        eventType.name(),
        getClass().getCanonicalName(),
        uniquify("blah").getBytes());
  }
}
