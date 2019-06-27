/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.alpha.fsm.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.servicecomb.pack.alpha.fsm.SagaActorState;

public class SagaData implements Serializable {
  private long beginTime = System.currentTimeMillis();
  private long endTime;
  private String globalTxId;
  private long expirationTime;
  private boolean terminated;
  private SagaActorState lastState;
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

  public SagaActorState getLastState() {
    return lastState;
  }

  public void setLastState(SagaActorState lastState) {
    this.lastState = lastState;
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
