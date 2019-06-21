package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.TxEvent;

public class TxAbortedEvent extends TxEvent {


  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxAbortedEvent txAbortedEvent;

    private Builder() {
      txAbortedEvent = new TxAbortedEvent();
    }

    public Builder parentTxId(String parentTxId) {
      txAbortedEvent.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txAbortedEvent.setLocalTxId(localTxId);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txAbortedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public TxAbortedEvent build() {
      return txAbortedEvent;
    }
  }
}
