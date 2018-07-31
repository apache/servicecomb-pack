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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.junit.Before;
import org.junit.Test;

public class SagaConsumerHandlerTest {

  private static final String globalTxId = UUID.randomUUID().toString();

  private static final String localTxId = UUID.randomUUID().toString();

  @SuppressWarnings("unchecked")
  private final IdGenerator<String> idGenerator = mock(IdGenerator.class);

  private final OmegaContext omegaContext = new OmegaContext(new IdGenerator<String>() {

    @Override
    public String nextId() {
      return "ignored";
    }
  });

  private final Invocation invocation = mock(Invocation.class);

  private final AsyncResponse asyncResponse = mock(AsyncResponse.class);

  private final SagaConsumerHandler handler = new SagaConsumerHandler(omegaContext);

  @Before
  public void setUp() {
    when(idGenerator.nextId()).thenReturn(globalTxId, localTxId);
  }

  @Test
  public void keepHeaderUnchangedIfContextAbsent() throws Exception {
    Map<String, String> context = new HashMap<>();
    when(invocation.getContext()).thenReturn(context);

    handler.handle(invocation, asyncResponse);

    assertThat(invocation.getContext().isEmpty(), is(true));
  }

  @Test
  public void interceptTransactionIdInHeaderIfContextPresent() throws Exception {
    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);

    Map<String, String> context = new HashMap<>();
    when(invocation.getContext()).thenReturn(context);

    handler.handle(invocation, asyncResponse);

    assertThat(invocation.getContext().get(OmegaContext.GLOBAL_TX_ID_KEY), is(globalTxId));
    assertThat(invocation.getContext().get(OmegaContext.LOCAL_TX_ID_KEY), is(localTxId));
  }
}
