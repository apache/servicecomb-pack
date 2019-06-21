package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.SagaEvent;

public class SagaTimeoutEvent extends SagaEvent {

  public static Builder builder() {
    return new Builder();
  }
  public static final class Builder {

    private SagaTimeoutEvent sagaTimeoutEvent;

    private Builder() {
      sagaTimeoutEvent = new SagaTimeoutEvent();
    }

    public Builder globalTxId(String globalTxId) {
      sagaTimeoutEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public SagaTimeoutEvent build() {
      return sagaTimeoutEvent;
    }
  }
}
