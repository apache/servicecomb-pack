package org.apache.servicecomb.pack.alpha.fsm.event;

import org.apache.servicecomb.pack.alpha.fsm.event.base.TxEvent;

public class TxComponsitedEvent extends TxEvent {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxComponsitedEvent txComponsitedEvent;

    private Builder() {
      txComponsitedEvent = new TxComponsitedEvent();
    }

    public Builder parentTxId(String parentTxId) {
      txComponsitedEvent.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txComponsitedEvent.setLocalTxId(localTxId);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txComponsitedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public TxComponsitedEvent build() {
      return txComponsitedEvent;
    }
  }
}
