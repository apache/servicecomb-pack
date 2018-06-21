/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transport.servicecomb;

import static java.util.Collections.emptyMap;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.junit.Before;
import org.junit.Test;

public class SagaProviderHandlerTest {

  private static final String globalTxId = UUID.randomUUID().toString();

  private static final String localTxId = UUID.randomUUID().toString();

  private final OmegaContext omegaContext = new OmegaContext(() -> "ignored");

  private final Invocation invocation = mock(Invocation.class);

  private final AsyncResponse asyncResponse = mock(AsyncResponse.class);

  private final SagaProviderHandler handler = new SagaProviderHandler(omegaContext);

  @Before
  public void setUp() {
    omegaContext.clear();
  }

  @Test
  public void setUpOmegaContextInTransactionRequest() throws Exception {
    Map<String, String> context = new HashMap<>();
    context.put(GLOBAL_TX_ID_KEY, globalTxId);
    context.put(LOCAL_TX_ID_KEY, localTxId);
    when(invocation.getContext()).thenReturn(context);

    handler.handle(invocation, asyncResponse);

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void doNothingInNonTransactionRequest() throws Exception {
    when(invocation.getContext()).thenReturn(emptyMap());

    handler.handle(invocation, asyncResponse);

    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }
}
