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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.model;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.servicecomb.pack.alpha.core.fsm.SuspendedType;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.SagaActorState;

public class SagaData implements Serializable {
  private String serviceName;
  private String instanceId;
  private Date beginTime = new Date();
  private Date endTime;
  private String globalTxId;
  private Date expirationTime;
  private int timeout;
  private SuspendedType suspendedType = SuspendedType.NONE;
  private boolean terminated;
  private SagaActorState lastState;
  private AtomicLong compensationRunningCounter = new AtomicLong();
  private TxEntities txEntities = new TxEntities();
  private List<BaseEvent> events = new LinkedList<>();

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public Date getBeginTime() {
    return beginTime;
  }

  public void setBeginTime(Date beginTime) {
    this.beginTime = beginTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public String getGlobalTxId() {
    return globalTxId;
  }

  public void setGlobalTxId(String globalTxId) {
    this.globalTxId = globalTxId;
  }

  public Date getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(Date expirationTime) {
    this.expirationTime = expirationTime;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public SuspendedType getSuspendedType() {
    return suspendedType;
  }

  public void setSuspendedType(SuspendedType suspendedType) {
    this.suspendedType = suspendedType;
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

  public TxEntities getTxEntities() {
    return txEntities;
  }

  public void setTxEntities(TxEntities txEntities) {
    this.txEntities = txEntities;
  }

  public SagaActorState getLastState() {
    return lastState;
  }

  public void setLastState(SagaActorState lastState) {
    this.lastState = lastState;
  }

  public long getTimeout(){
    return expirationTime.getTime()-beginTime.getTime();
  }

  public void logEvent(BaseEvent event){
    this.events.add(event);
  }

  public List<BaseEvent> getEvents() {
    return events;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private SagaData sagaData;

    private Builder() {
      sagaData = new SagaData();
    }

    public Builder beginTime(Date beginTime) {
      sagaData.setBeginTime(beginTime);
      return this;
    }

    public Builder endTime(Date endTime) {
      sagaData.setEndTime(endTime);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      sagaData.setGlobalTxId(globalTxId);
      return this;
    }

    public Builder expirationTime(Date expirationTime) {
      sagaData.setExpirationTime(expirationTime);
      return this;
    }

    public Builder timeout(int timeout) {
      sagaData.setTimeout(timeout);
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

    public Builder txEntities(TxEntities txEntities) {
      sagaData.setTxEntities(txEntities);
      return this;
    }
    public Builder serviceName(String serviceName) {
      sagaData.setServiceName(serviceName);
      return this;
    }

    public Builder instanceId(String instanceId) {
      sagaData.setInstanceId(instanceId);
      return this;
    }

    public Builder suspendedType(SuspendedType suspendedType) {
      sagaData.setSuspendedType(suspendedType);
      return this;
    }

    public SagaData build() {
      return sagaData;
    }
  }
}
