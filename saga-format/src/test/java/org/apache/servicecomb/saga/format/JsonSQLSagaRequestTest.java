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

import org.junit.Test;
import org.mockito.Mockito;

public class JsonSQLSagaRequestTest {

  private final JacksonSQLTransaction transaction = Mockito.mock(JacksonSQLTransaction.class);
  private final JacksonSQLCompensation compensation = Mockito.mock(JacksonSQLCompensation.class);

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

  private JsonSQLSagaRequest newSagaRequest(
      JacksonSQLTransaction transaction,
      JacksonSQLCompensation compensation,
      JacksonFallback fallback) {

    return new JsonSQLSagaRequest(
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
