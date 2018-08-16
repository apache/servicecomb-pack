package org.apache.servicecomb.saga.omega.transaction.tcc.events;

public class TccEndEvent {
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
   

  public TccEndEvent(String globalTxId, String localTxId, String parentTxId) {
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
  }
}
