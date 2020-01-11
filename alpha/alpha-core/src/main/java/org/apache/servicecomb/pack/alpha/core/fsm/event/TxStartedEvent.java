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
  private int forwardRetries;
  private int forwardTimeout;
  private int reverseRetries;
  private int reverseTimeout;
  private int retryDelayInMilliseconds;

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

  public int getForwardRetries() {
    return forwardRetries;
  }

  public void setForwardRetries(int forwardRetries) {
    this.forwardRetries = forwardRetries;
  }

  public int getForwardTimeout() {
    return forwardTimeout;
  }

  public void setForwardTimeout(int forwardTimeout) {
    this.forwardTimeout = forwardTimeout;
  }

  public int getReverseRetries() {
    return reverseRetries;
  }

  public void setReverseRetries(int reverseRetries) {
    this.reverseRetries = reverseRetries;
  }

  public int getReverseTimeout() {
    return reverseTimeout;
  }

  public void setReverseTimeout(int reverseTimeout) {
    this.reverseTimeout = reverseTimeout;
  }

  public int getRetryDelayInMilliseconds() {
    return retryDelayInMilliseconds;
  }

  public void setRetryDelayInMilliseconds(int retryDelayInMilliseconds) {
    this.retryDelayInMilliseconds = retryDelayInMilliseconds;
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

    public Builder forwardRetries(int forwardRetries) {
      txStartedEvent.setForwardRetries(forwardRetries);
      return this;
    }

    public Builder forwardTimeout(int forwardTimeout) {
      txStartedEvent.setForwardTimeout(forwardTimeout);
      return this;
    }

    public Builder reverseRetries(int reverseRetries) {
      txStartedEvent.setReverseRetries(reverseRetries);
      return this;
    }

    public Builder reverseTimeout(int reverseTimeout) {
      txStartedEvent.setReverseTimeout(reverseTimeout);
      return this;
    }

    public Builder retryDelayInMilliseconds(int retryDelayInMilliseconds) {
      txStartedEvent.setRetryDelayInMilliseconds(retryDelayInMilliseconds);
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
