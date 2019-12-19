package org.apache.servicecomb.pack.integration.tests.explicitcontext;

import org.apache.servicecomb.pack.omega.context.TransactionContext;

public class TransactionContextDto {

  private String globalTxId;
  private String localTxId;

  public String getGlobalTxId() {
    return globalTxId;
  }

  public void setGlobalTxId(String globalTxId) {
    this.globalTxId = globalTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId = localTxId;
  }

  public TransactionContext convertBack() {
    return new TransactionContext(globalTxId, localTxId);
  }

  public static TransactionContextDto convert(TransactionContext transactionContext) {
    TransactionContextDto transactionContextDto = new TransactionContextDto();
    transactionContextDto.setGlobalTxId(transactionContext.globalTxId());
    transactionContextDto.setLocalTxId(transactionContext.localTxId());
    return transactionContextDto;
  }
}
