package org.apache.servicecomb.pack.alpha.fsm.model;


import java.io.Serializable;
import org.apache.servicecomb.pack.alpha.fsm.TxState;

public class TxEntity implements Serializable {
  private long beginTime = System.currentTimeMillis();
  private long endTime;
  private String parentTxId;
  private String localTxId;
  private TxState state;

  public long getBeginTime() {
    return beginTime;
  }

  public void setBeginTime(long beginTime) {
    this.beginTime = beginTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public String getParentTxId() {
    return parentTxId;
  }

  public void setParentTxId(String parentTxId) {
    this.parentTxId = parentTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId = localTxId;
  }

  public TxState getState() {
    return state;
  }

  public void setState(TxState state) {
    this.state = state;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxEntity txEntity;

    private Builder() {
      txEntity = new TxEntity();
    }

    public Builder beginTime(long beginTime) {
      txEntity.setBeginTime(beginTime);
      return this;
    }

    public Builder endTime(long endTime) {
      txEntity.setEndTime(endTime);
      return this;
    }

    public Builder parentTxId(String parentTxId) {
      txEntity.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txEntity.setLocalTxId(localTxId);
      return this;
    }

    public Builder state(TxState state) {
      txEntity.setState(state);
      return this;
    }

    public TxEntity build() {
      return txEntity;
    }
  }
}
