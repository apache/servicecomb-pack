package org.apache.servicecomb.saga.omega.transaction.tcc.events;

public class TccStartEvent {
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;

  public String getGlobalTxId() {
    return globalTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public String getParentTxId() {
    return parentTxId;
  }

  public TccStartEvent(String globalTxId, String localTxId, String parentTxId) {
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
  }
}
