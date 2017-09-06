/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.seanyinx.github.unit.scaffolding.AssertUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RetryableCompensationTest {

  private final int numberOfRetries = 3;
  private final String address = uniquify("address");

  private final SagaResponse success = Mockito.mock(SagaResponse.class);
  private final SagaResponse failure = Mockito.mock(SagaResponse.class);
  private final Fallback fallback = Mockito.mock(Fallback.class);
  private final Compensation transport = Mockito.mock(Compensation.class);
  private final Compensation retryableTransport = new RetryableCompensation(numberOfRetries, 100, transport, fallback);

  @Before
  public void setUp() throws Exception {
    when(fallback.fallback()).thenReturn(failure);
  }

  @Test
  public void retriesTransportForSpecifiedTimes() {
    TransactionFailedException exception = new TransactionFailedException("oops");

    when(transport.send(address))
        .thenThrow(exception)
        .thenThrow(exception)
        .thenReturn(success);

    SagaResponse response = retryableTransport.send(address);

    assertThat(response, is(success));
    verify(transport, times(3)).send(address);
  }

  @Test
  public void fallbackIfTransportFailedWithRetry() {
    TransactionFailedException exception = new TransactionFailedException("oops");

    when(transport.send(address)).thenThrow(exception);

    SagaResponse response = retryableTransport.send(address);
    assertThat(response, is(failure));

    verify(transport, times(numberOfRetries)).send(address);
    verify(fallback).fallback();
  }

  @Test
  public void blowsUpIfNumberOfRetriesIsLessThanOne() {
    try {
      new RetryableCompensation(0, 100, transport, fallback);
      AssertUtils.expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("The number of retries must be greater than 0"));
    }
  }
}