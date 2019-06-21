package org.apache.servicecomb.pack.alpha.fsm.event.base;

import java.io.Serializable;

public abstract class BaseEvent implements Serializable {
  private String globalTxId;

  public BaseEvent() {

  }

  public String getGlobalTxId() {
    return globalTxId;
  }

  public void setGlobalTxId(String globalTxId) {
    this.globalTxId = globalTxId;
  }

  @Override
  public String toString() {
    return "BaseEvent{" +
        "globalTxId='" + globalTxId + '\'' +
        '}';
  }
}
