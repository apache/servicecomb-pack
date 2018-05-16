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

import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.rpc.Invocation;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transport.dubbo.SagaDubboProviderFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SagaDubboProviderFilterTest {

  private static final String globalTxId = UUID.randomUUID().toString();
  private static final String localTxId = UUID.randomUUID().toString();
  private final OmegaContext omegaContext = new OmegaContext(() -> "ignored");
  private final Invocation invocation = mock(Invocation.class);
  private final ApplicationContext applicationContext = mock(ApplicationContext.class);

  private final SagaDubboProviderFilter filter = new SagaDubboProviderFilter();

  @Before
  public void setUp() {
    omegaContext.clear();
    when(applicationContext.containsBean("omegaContext")).thenReturn(true);
    when(applicationContext.getBean("omegaContext")).thenReturn(omegaContext);
    SpringExtensionFactory.addApplicationContext(applicationContext);
  }

  @After
  public void setDown(){
    SpringExtensionFactory.removeApplicationContext(applicationContext);
  }

  @Test
  public void setUpOmegaContextInTransactionRequest() throws Exception {
    when(invocation.getAttachment(OmegaContext.GLOBAL_TX_ID_KEY)).thenReturn(globalTxId);
    when(invocation.getAttachment(OmegaContext.LOCAL_TX_ID_KEY)).thenReturn(localTxId);

    filter.invoke(null, invocation);

    assertThat(omegaContext.globalTxId(), is(globalTxId));
    assertThat(omegaContext.localTxId(), is(localTxId));
  }

  @Test
  public void doNothingInNonTransactionRequest() throws Exception {
    when(invocation.getAttachment(OmegaContext.GLOBAL_TX_ID_KEY)).thenReturn(null);
    when(invocation.getAttachment(OmegaContext.LOCAL_TX_ID_KEY)).thenReturn(null);

    filter.invoke(null, invocation);

    assertThat(omegaContext.globalTxId(), is(nullValue()));
    assertThat(omegaContext.localTxId(), is(nullValue()));
  }

}
