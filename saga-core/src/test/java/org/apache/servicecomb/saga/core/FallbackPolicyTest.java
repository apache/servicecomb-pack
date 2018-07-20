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

package org.apache.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FallbackPolicyTest {

  private final int numberOfRetries = 3;
  private final String address = uniquify("address");

  private final SagaResponse success = Mockito.mock(SagaResponse.class);
  private final SagaResponse failure = Mockito.mock(SagaResponse.class);
  private final Fallback fallback = Mockito.mock(Fallback.class);
  private final Compensation compensation = Mockito.mock(Compensation.class);
  private final FallbackPolicy fallbackPolicy = new FallbackPolicy(100);

  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private final RuntimeException exception = new RuntimeException("oops");

  @Before
  public void setUp() throws Exception {
    when(compensation.retries()).thenReturn(numberOfRetries);
    when(fallback.send(address)).thenReturn(failure);
  }

  @Test
  public void retriesTransportForSpecifiedTimes() {
    when(compensation.send(address))
        .thenThrow(exception)
        .thenThrow(exception)
        .thenReturn(success);

    SagaResponse response = fallbackPolicy.apply(address, compensation, fallback);

    assertThat(response, is(success));
    verify(compensation, times(3)).send(address);
  }

  @Test
  public void fallbackIfTransportFailedWithRetry() {
    when(compensation.send(address)).thenThrow(exception);

    SagaResponse response = fallbackPolicy.apply(address, compensation, fallback);
    assertThat(response, is(failure));

    verify(compensation, times(numberOfRetries + 1)).send(address);
    verify(fallback).send(address);
  }

  @Test
  public void retryUntilSuccessIfNumberOfRetriesIsNegative() throws InterruptedException {
    reset(compensation);
    when(compensation.retries()).thenReturn(-1);
    when(compensation.send(address))
        .thenThrow(exception, exception, exception, exception, exception)
        .thenReturn(success);

    SagaResponse response = fallbackPolicy.apply(address, compensation, fallback);

    assertThat(response, is(success));
    verify(fallback, never()).send(anyString());
  }
}
