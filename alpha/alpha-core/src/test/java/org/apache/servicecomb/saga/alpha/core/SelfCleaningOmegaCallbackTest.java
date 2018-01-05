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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SelfCleaningOmegaCallbackTest {
  private final TxEvent event = Mockito.mock(TxEvent.class);
  private final String someId = uniquify("someId");

  private final Map<String, OmegaCallback> callbacks = new HashMap<>();
  private final OmegaCallback underlying = Mockito.mock(OmegaCallback.class);
  private final SelfCleaningOmegaCallback callback = new SelfCleaningOmegaCallback(someId, underlying, callbacks);

  @Before
  public void setUp() throws Exception {
    callbacks.put(someId, callback);
  }

  @Test
  public void keepItselfInCallbacksWhenNormal() throws Exception {
    callback.compensate(event);

    assertThat(callbacks.get(someId), is(callback));
  }

  @Test
  public void removeItselfFromCallbacksOnException() throws Exception {
    doThrow(RuntimeException.class).when(underlying).compensate(any(TxEvent.class));

    try {
      callback.compensate(event);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException ignored) {
    }

    assertThat(callbacks.isEmpty(), is(true));
  }

  @Test
  public void disconnectWithUnderlying() throws Exception {
    callback.disconnect();

    verify(underlying).disconnect();
  }
}
