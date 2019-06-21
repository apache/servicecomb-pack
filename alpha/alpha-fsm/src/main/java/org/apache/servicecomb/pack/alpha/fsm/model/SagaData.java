package org.apache.servicecomb.pack.alpha.fsm.model;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SagaData implements Serializable {
  private long beginTime = System.currentTimeMillis();
  private long endTime;
  private String globalTxId;
  private long expirationTime;
  private boolean terminated;
  private AtomicLong compensationRunningCounter = new AtomicLong();
  private Map<String,TxEntity> txEntityMap = new HashMap<>();

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

  public String getGlobalTxId() {
    return globalTxId;
  }

  public void setGlobalTxId(String globalTxId) {
    this.globalTxId = globalTxId;
  }

  public long getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(long expirationTime) {
    this.expirationTime = expirationTime;
  }

  public boolean isTerminated() {
    return terminated;
  }

  public void setTerminated(boolean terminated) {
    this.terminated = terminated;
  }

  public AtomicLong getCompensationRunningCounter() {
    return compensationRunningCounter;
  }

  public void setCompensationRunningCounter(
      AtomicLong compensationRunningCounter) {
    this.compensationRunningCounter = compensationRunningCounter;
  }

  public Map<String, TxEntity> getTxEntityMap() {
    return txEntityMap;
  }

  public void setTxEntityMap(
      Map<String, TxEntity> txEntityMap) {
    this.txEntityMap = txEntityMap;
  }

  public long getTimeout(){
    return expirationTime-System.currentTimeMillis();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private SagaData sagaData;

    private Builder() {
      sagaData = new SagaData();
    }

    public Builder beginTime(long beginTime) {
      sagaData.setBeginTime(beginTime);
      return this;
    }

    public Builder endTime(long endTime) {
      sagaData.setEndTime(endTime);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      sagaData.setGlobalTxId(globalTxId);
      return this;
    }

    public Builder expirationTime(long expirationTime) {
      sagaData.setExpirationTime(expirationTime);
      return this;
    }

    public Builder terminated(boolean terminated) {
      sagaData.setTerminated(terminated);
      return this;
    }

    public Builder compensationRunningCounter(AtomicLong compensationRunningCounter) {
      sagaData.setCompensationRunningCounter(compensationRunningCounter);
      return this;
    }

    public Builder txEntityMap(Map<String, TxEntity> txEntityMap) {
      sagaData.setTxEntityMap(txEntityMap);
      return this;
    }

    public SagaData build() {
      return sagaData;
    }
  }
}
