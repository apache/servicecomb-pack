package org.apache.servicecomb.pack.alpha.fsm.event.base;

public abstract class TxEvent extends BaseEvent {
  private String parentTxId;
  private String localTxId;

  public String getParentTxId() {
    return parentTxId;
  }

  public void setParentTxId(String parentTxId) {
    this.parentTxId = parentTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId = localTxId;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" +
        "globalTxId='" + this.getGlobalTxId() + '\'' +
        "parentTxId='" + parentTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        '}';
  }
}
