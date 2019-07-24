package org.apache.servicecomb.pack.omega.transaction;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.TransactionContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.slf4j.Logger;

public abstract class TransactionContextUtils {
  private TransactionContextUtils() {
    // singleton
  }
  public static TransactionContext extractTransactionContext(Object[] args) {
    if (args != null && args.length > 0) {
      for (Object arg : args) {
        // check the TransactionContext first
        if (arg instanceof TransactionContext) {
          return (TransactionContext) arg;
        }
        if (arg instanceof TransactionContextProperties) {
          TransactionContextProperties transactionContextProperties = (TransactionContextProperties) arg;
          return new TransactionContext(transactionContextProperties.getGlobalTxId(), transactionContextProperties.getLocalTxId());
        }
      }
    }
    return null;
  }

  public static void populateOmegaContext(OmegaContext context, TransactionContext transactionContext, Logger logger) {
    if (context.globalTxId() != null) {
      logger.warn("The context {}'s globalTxId is not empty. Update it for globalTxId:{} and localTxId:{}", context,
        transactionContext.globalTxId(), transactionContext.localTxId());
    } else {
      logger.debug("Updated context {} for globalTxId:{} and localTxId:{}", context,
        transactionContext.globalTxId(), transactionContext.localTxId());
    }
    context.setGlobalTxId(transactionContext.globalTxId());
    context.setLocalTxId(transactionContext.localTxId());
  }

}
