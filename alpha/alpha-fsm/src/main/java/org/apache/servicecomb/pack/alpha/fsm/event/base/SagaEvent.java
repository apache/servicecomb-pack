package org.apache.servicecomb.pack.alpha.fsm.event.base;

public class SagaEvent extends BaseEvent {

  public SagaEvent() {
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" +
        "globalTxId='" + this.getGlobalTxId() + '\'' +
        '}';
  }
}
