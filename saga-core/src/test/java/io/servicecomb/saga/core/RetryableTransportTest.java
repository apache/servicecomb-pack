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
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RetryableTransportTest {

  private final int numberOfRetries = 3;
  private final String address = uniquify("address");
  private final String path = uniquify("path");
  private final String method = uniquify("method");
  private final Map<String, Map<String, String>> params = Collections.emptyMap();

  private final SagaResponse success = Mockito.mock(SagaResponse.class);
  private final SagaResponse failure = Mockito.mock(SagaResponse.class);
  private final Fallback fallback = Mockito.mock(Fallback.class);
  private final Transport transport = Mockito.mock(Transport.class);
  private final Transport retryableTransport = new RetryableTransport(numberOfRetries, transport, fallback);

  @Before
  public void setUp() throws Exception {
    when(fallback.fallback()).thenReturn(failure);
  }

  @Test
  public void retriesTransportForSpecifiedTimes() {
    TransactionFailedException exception = new TransactionFailedException("oops");

    when(transport.with(address, path, method, params))
        .thenThrow(exception)
        .thenThrow(exception)
        .thenReturn(success);

    SagaResponse response = retryableTransport.with(address, path, method, params);

    assertThat(response, is(success));
    verify(transport, times(3)).with(address, path, method, params);
  }

  @Test
  public void fallbackIfTransportFailedWithRetry() {
    TransactionFailedException exception = new TransactionFailedException("oops");

    when(transport.with(address, path, method, params)).thenThrow(exception);

    SagaResponse response = retryableTransport.with(address, path, method, params);
    assertThat(response, is(failure));

    verify(transport, times(numberOfRetries)).with(address, path, method, params);
    verify(fallback).fallback();
  }

  @Test
  public void blowsUpIfNumberOfRetriesIsLessThanOne() {
    try {
      new RetryableTransport(0, transport, fallback);
      AssertUtils.expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("The number of retries must be greater than 0"));
    }
  }
}