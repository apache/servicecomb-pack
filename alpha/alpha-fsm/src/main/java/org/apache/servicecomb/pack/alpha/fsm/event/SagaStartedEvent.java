package org.apache.servicecomb.pack.alpha.fsm.event;


import org.apache.servicecomb.pack.alpha.fsm.event.base.SagaEvent;

public class SagaStartedEvent extends SagaEvent {
  private int timeout; //second

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private SagaStartedEvent sagaStartedEvent;

    private Builder() {
      sagaStartedEvent = new SagaStartedEvent();
    }

    public Builder globalTxId(String globalTxId) {
      sagaStartedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public Builder timeout(int timeout) {
      sagaStartedEvent.setTimeout(timeout);
      return this;
    }

    public SagaStartedEvent build() {
      return sagaStartedEvent;
    }
  }
}
