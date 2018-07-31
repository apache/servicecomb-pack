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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transport.dubbo.SagaDubboConsumerFilter;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.rpc.Invocation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SagaDubboConsumerFilterTest {

  private static final String globalTxId = UUID.randomUUID().toString();
  private static final String localTxId = UUID.randomUUID().toString();

  private final OmegaContext omegaContext = new OmegaContext(new IdGenerator<String>() {

    @Override
    public String nextId() {
      return "ignored";
    }
  });
  private final Invocation invocation = mock(Invocation.class);
  private final SagaDubboConsumerFilter filter = new SagaDubboConsumerFilter();

  @Before
  public void setUp() {
    omegaContext.clear();
    filter.setOmegaContext(omegaContext);
  }

  @Test
  public void keepHeaderUnchangedIfContextAbsent() {
    when(invocation.getAttachment(OmegaContext.GLOBAL_TX_ID_KEY)).thenReturn(null);
    when(invocation.getAttachment(OmegaContext.LOCAL_TX_ID_KEY)).thenReturn(null);

    filter.invoke(null, invocation);

    assertThat(invocation.getAttachments().isEmpty(), is(true));
  }

  @Test
  public void interceptTransactionIdInHeaderIfContextPresent() {
    omegaContext.setGlobalTxId(globalTxId);
    omegaContext.setLocalTxId(localTxId);

    Map<String, String> attachMents = new HashMap<>();
    when(invocation.getAttachments()).thenReturn(attachMents);

    filter.invoke(null, invocation);

    assertThat(invocation.getAttachments().get(OmegaContext.GLOBAL_TX_ID_KEY), is(globalTxId));
    assertThat(invocation.getAttachments().get(OmegaContext.LOCAL_TX_ID_KEY), is(localTxId));
  }
}
