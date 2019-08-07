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
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;

public class TxEntity implements Serializable {
  private String serviceName;
  private String instanceId;
  private String globalTxId;
  private Date beginTime = new Date();
  private Date endTime;
  private String parentTxId;
  private String localTxId;
  private TxState state;
  private String compensationMethod;
  private byte[] payloads;
  private byte[] throwablePayLoads;
  private int retries;
  private AtomicInteger retriesCounter = new AtomicInteger();

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

  public String getCompensationMethod() {
    return compensationMethod;
  }

  public void setCompensationMethod(String compensationMethod) {
    this.compensationMethod = compensationMethod;
  }

  public byte[] getPayloads() {
    return payloads;
  }

  public void setPayloads(byte[] payloads) {
    this.payloads = payloads;
  }

  public byte[] getThrowablePayLoads() {
    return throwablePayLoads;
  }

  public void setThrowablePayLoads(byte[] throwablePayLoads) {
    this.throwablePayLoads = throwablePayLoads;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public AtomicInteger getRetriesCounter() {
    return retriesCounter;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxEntity txEntity;

    private Builder() {
      txEntity = new TxEntity();
    }

    public Builder beginTime(Date beginTime) {
      txEntity.setBeginTime(beginTime);
      return this;
    }

    public Builder endTime(Date endTime) {
      txEntity.setEndTime(endTime);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txEntity.setGlobalTxId(globalTxId);
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

    public Builder compensationMethod(String compensationMethod) {
      txEntity.setCompensationMethod(compensationMethod);
      return this;
    }

    public Builder payloads(byte[] payloads) {
      txEntity.setPayloads(payloads);
      return this;
    }

    public Builder throwablePayLoads(byte[] throwablePayLoads) {
      txEntity.setThrowablePayLoads(throwablePayLoads);
      return this;
    }

    public Builder state(TxState state) {
      txEntity.setState(state);
      return this;
    }

    public Builder serviceName(String serviceName) {
      txEntity.setServiceName(serviceName);
      return this;
    }

    public Builder instanceId(String instanceId) {
      txEntity.setInstanceId(instanceId);
      return this;
    }

    public Builder retries(int retries) {
      txEntity.setRetries(retries);
      return this;
    }

    public TxEntity build() {
      return txEntity;
    }
  }
}
