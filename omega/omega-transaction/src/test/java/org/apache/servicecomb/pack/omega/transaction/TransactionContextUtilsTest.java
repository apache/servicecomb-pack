package org.apache.servicecomb.pack.omega.transaction;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.TransactionContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.UUID;

import static org.apache.servicecomb.pack.omega.transaction.TransactionContextUtils.extractTransactionContext;
import static org.apache.servicecomb.pack.omega.transaction.TransactionContextUtils.populateOmegaContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionContextUtilsTest {

  private final Logger logger = mock(Logger.class);

  private final String transactionGlobalTxId = UUID.randomUUID().toString();
  private final String transactionLocalTxId = UUID.randomUUID().toString();

  private final TransactionContext txContext = new TransactionContext(transactionGlobalTxId, transactionLocalTxId);
  private final TransactionContextProperties txContextProperties = mock(TransactionContextProperties.class);

  @Before
  public void setUp() {
    when(txContextProperties.getGlobalTxId()).thenReturn(transactionGlobalTxId);
    when(txContextProperties.getLocalTxId()).thenReturn(transactionLocalTxId);
  }

  @Test
  public void testExtractTransactionContext() {

    TransactionContext result = extractTransactionContext(new Object[] { txContextProperties });
    assertThat(result.globalTxId(), is(transactionGlobalTxId));
    assertThat(result.localTxId(), is(transactionLocalTxId));

    result = extractTransactionContext(new Object[] {});
    assertNull(result);

    result = extractTransactionContext(null);
    assertNull(result);

    result = extractTransactionContext(new Object[] { txContext });
    assertThat(result, is(txContext));

    TransactionContext otherTx = Mockito.mock(TransactionContext.class);
    result = extractTransactionContext(new Object[] { otherTx, txContextProperties });
    assertThat(result, is(otherTx));
  }

  @Test
  public void testPopulateOmegaContextWhenItsEmpty() {

    OmegaContext omegaContext = new OmegaContext(null);

    populateOmegaContext(omegaContext, txContext, logger);

    assertEquals(transactionGlobalTxId, omegaContext.globalTxId());
    assertEquals(transactionLocalTxId, omegaContext.localTxId());

  }

  @Test
  public void testPopulateOmegaContextWhenItsNotEmpty() {
    OmegaContext omegaContext = new OmegaContext(null);

    omegaContext.setGlobalTxId("global-tx-id");
    omegaContext.setLocalTxId("local-tx-id");

    populateOmegaContext(omegaContext, txContext, logger);

    assertEquals(transactionGlobalTxId, omegaContext.globalTxId());
    assertEquals(transactionLocalTxId, omegaContext.localTxId());

  }

}
