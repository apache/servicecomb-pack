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

package io.servicecomb.saga.alpha.core;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;

public class RetryOmegaCallbackTest {
  private final int delay = 100;
  private final OmegaCallback underlying = Mockito.mock(OmegaCallback.class);
  private final RetryOmegaCallback callback = new RetryOmegaCallback(underlying, delay);

  @Test
  public void retryOnFailure() throws Exception {
    TxEvent event = someEvent();

    doThrow(AlphaException.class)
        .doThrow(AlphaException.class)
        .doNothing()
        .when(underlying)
        .compensate(event);

    callback.compensate(event);

    verify(underlying, times(3)).compensate(event);
  }

  @Test
  public void exitOnInterruption() throws Exception {
    TxEvent event = someEvent();

    doThrow(AlphaException.class).when(underlying).compensate(event);

    Thread thread = new Thread(() -> callback.compensate(event));
    thread.start();

    Thread.sleep(300);
    thread.interrupt();

    verify(underlying, atMost(4)).compensate(event);
  }

  private TxEvent someEvent() {
    return new TxEvent(
        uniquify("serviceName"),
        uniquify("instanceId"),
        new Date(),
        uniquify("globalTxId"),
        uniquify("localTxId"),
        UUID.randomUUID().toString(),
        EventType.TxStartedEvent.name(),
        getClass().getCanonicalName(),
        uniquify("blah").getBytes());
  }
}
