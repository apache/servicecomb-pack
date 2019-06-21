package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.TxEvent;

public class TxEndedEvent extends TxEvent {

  public static Builder builder() {
    return new Builder();
  }


  public static final class Builder {

    private TxEndedEvent txEndedEvent;

    private Builder() {
      txEndedEvent = new TxEndedEvent();
    }

    public Builder parentTxId(String parentTxId) {
      txEndedEvent.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txEndedEvent.setLocalTxId(localTxId);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txEndedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public TxEndedEvent build() {
      return txEndedEvent;
    }
  }
}
