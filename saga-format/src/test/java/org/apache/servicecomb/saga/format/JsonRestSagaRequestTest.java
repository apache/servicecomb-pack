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

package org.apache.servicecomb.saga.format;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.apache.servicecomb.saga.core.Operation.TYPE_REST;
import static org.apache.servicecomb.saga.format.JacksonFallback.NOP_TRANSPORT_AWARE_FALLBACK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.servicecomb.saga.transports.TransportFactory;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.servicecomb.saga.transports.RestTransport;

public class JsonRestSagaRequestTest {

  private final RestTransport restTransport = Mockito.mock(RestTransport.class);
  private final TransportFactory transportFactory = Mockito.mock(TransportFactory.class);
  private final JacksonRestTransaction transaction = Mockito.mock(JacksonRestTransaction.class);
  private final JacksonRestCompensation compensation = Mockito.mock(JacksonRestCompensation.class);

  @Test
  public void blowsUpIfTransactionIsNotSpecified() {
    try {
      newSagaRequest(null, compensation, NOP_TRANSPORT_AWARE_FALLBACK);

      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Invalid request with NO transaction specified"));
    }
  }

  @Test
  public void blowsUpIfCompensationIsNotSpecified() {
    try {
      newSagaRequest(transaction, null, NOP_TRANSPORT_AWARE_FALLBACK);

      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Invalid request with NO compensation specified"));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultToNopFallbackIfNotSpecified() {
    when(transportFactory.getTransport()).thenReturn(restTransport);
    JsonRestSagaRequest request = newSagaRequest(transaction, compensation, null);

    request.with(transportFactory);

    request.fallback().send(uniquify("blah"));

    verify(restTransport, never()).with(anyString(), anyString(), anyString(), anyStringMap());
  }

  private Map<String, Map<String, String>> anyStringMap() {
    return anyMap();
  }

  private JsonRestSagaRequest newSagaRequest(
      JacksonRestTransaction transaction,
      JacksonRestCompensation compensation,
      JacksonFallback fallback) {

    return new JsonRestSagaRequest(
        uniquify("id"),
        uniquify("serviceName"),
        TYPE_REST,
        transaction,
        compensation,
        fallback,
        null,
        0);
  }
}
