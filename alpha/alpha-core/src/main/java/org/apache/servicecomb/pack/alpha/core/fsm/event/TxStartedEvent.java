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

package org.apache.servicecomb.pack.alpha.core.fsm.event;

import java.util.Date;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.TxEvent;

public class TxStartedEvent extends TxEvent {
  private String compensationMethod;
  private byte[] payloads;
  private String retryMethod;
  private int retries;

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

  public String getRetryMethod() {
    return retryMethod;
  }

  public void setRetryMethod(String retryMethod) {
    this.retryMethod = retryMethod;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

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

    public Builder compensationMethod(String compensationMethod) {
      txStartedEvent.setCompensationMethod(compensationMethod);
      return this;
    }

    public Builder payloads(byte[] payloads) {
      txStartedEvent.setPayloads(payloads);
      return this;
    }

    public Builder serviceName(String serviceName) {
      txStartedEvent.setServiceName(serviceName);
      return this;
    }

    public Builder instanceId(String instanceId) {
      txStartedEvent.setInstanceId(instanceId);
      return this;
    }

    public Builder retryMethod(String retryMethod) {
      txStartedEvent.setRetryMethod(retryMethod);
      return this;
    }

    public Builder retries(int retries) {
      txStartedEvent.setRetries(retries);
      return this;
    }

    public Builder createTime(Date createTime){
      txStartedEvent.setCreateTime(createTime);
      return this;
    }

    public TxStartedEvent build() {
      return txStartedEvent;
    }
  }
}
