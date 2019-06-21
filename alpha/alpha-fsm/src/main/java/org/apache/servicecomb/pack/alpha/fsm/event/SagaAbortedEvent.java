package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.SagaEvent;

public class SagaAbortedEvent extends SagaEvent {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private SagaAbortedEvent sagaAbortedEvent;

    private Builder() {
      sagaAbortedEvent = new SagaAbortedEvent();
    }

    public Builder globalTxId(String globalTxId) {
      sagaAbortedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public SagaAbortedEvent build() {
      return sagaAbortedEvent;
    }
  }
}
