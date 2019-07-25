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

package org.apache.servicecomb.pack.omega.transaction;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.TransactionContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionContextHelperTest {

  private final String transactionGlobalTxId = UUID.randomUUID().toString();

  private final String transactionLocalTxId = UUID.randomUUID().toString();

  private final TransactionContext txContext = new TransactionContext(transactionGlobalTxId, transactionLocalTxId);

  private final TransactionContextProperties txContextProperties = mock(TransactionContextProperties.class);

  private final TransactionContextHelper transactionContextHelper = new TransactionContextHelper() {
    @Override
    protected Logger getLogger() {
      return LoggerFactory.getLogger(getClass());
    }
  };

  @Before
  public void setUp() {
    when(txContextProperties.getGlobalTxId()).thenReturn(transactionGlobalTxId);
    when(txContextProperties.getLocalTxId()).thenReturn(transactionLocalTxId);
  }

  @Test
  public void testExtractTransactionContext() {

    TransactionContext result = transactionContextHelper.extractTransactionContext(new Object[] {txContextProperties});
    assertThat(result.globalTxId(), is(transactionGlobalTxId));
    assertThat(result.localTxId(), is(transactionLocalTxId));

    result = transactionContextHelper.extractTransactionContext(new Object[] {});
    assertNull(result);

    result = transactionContextHelper.extractTransactionContext(null);
    assertNull(result);

    result = transactionContextHelper.extractTransactionContext(new Object[] {txContext});
    assertThat(result, is(txContext));

    TransactionContext otherTx = Mockito.mock(TransactionContext.class);
    result = transactionContextHelper.extractTransactionContext(new Object[] {otherTx, txContextProperties});
    assertThat(result, is(otherTx));
  }

  @Test
  public void testPopulateOmegaContextWhenItsEmpty() {

    OmegaContext omegaContext = new OmegaContext(null);

    transactionContextHelper.populateOmegaContext(omegaContext, txContext);

    assertEquals(transactionGlobalTxId, omegaContext.globalTxId());
    assertEquals(transactionLocalTxId, omegaContext.localTxId());
  }

  @Test
  public void testPopulateOmegaContextWhenItsNotEmpty() {
    OmegaContext omegaContext = new OmegaContext(null);

    omegaContext.setGlobalTxId("global-tx-id");
    omegaContext.setLocalTxId("local-tx-id");

    transactionContextHelper.populateOmegaContext(omegaContext, txContext);

    assertEquals(transactionGlobalTxId, omegaContext.globalTxId());
    assertEquals(transactionLocalTxId, omegaContext.localTxId());
  }
}
