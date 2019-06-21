package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.SagaEvent;

public class SagaEndedEvent extends SagaEvent {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private SagaEndedEvent sagaEndedEvent;

    private Builder() {
      sagaEndedEvent = new SagaEndedEvent();
    }

    public Builder globalTxId(String globalTxId) {
      sagaEndedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public SagaEndedEvent build() {
      return sagaEndedEvent;
    }
  }
}
