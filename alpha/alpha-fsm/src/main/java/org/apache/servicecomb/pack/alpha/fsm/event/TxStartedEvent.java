package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.TxEvent;

public class TxStartedEvent extends TxEvent {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxStartedEvent txStartedEvent;

    private Builder() {
      txStartedEvent = new TxStartedEvent();
    }

    public Builder parentTxId(String parentTxId) {
      txStartedEvent.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txStartedEvent.setLocalTxId(localTxId);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txStartedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public TxStartedEvent build() {
      return txStartedEvent;
    }
  }
}
