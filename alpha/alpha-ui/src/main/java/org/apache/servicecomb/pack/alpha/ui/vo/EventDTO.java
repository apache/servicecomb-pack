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

package org.apache.servicecomb.pack.alpha.ui.vo;

import java.util.Date;

public class EventDTO {
  private String type;
  private String globalTxId;
  private String serviceName;
  private String instanceId;
  private String parentTxId;
  private String localTxId;
  private Date createTime;
  private long timeout;
  private long reverseRetries;
  private long forwardRetries;
  private long reverseTimeout;
  private long forwardTimeout;
  private long retryDelayInMilliseconds;
  private String compensationMethod;
  private String exception;

  public String getType() {
    return type;
  }

  public String getGlobalTxId() {
    return globalTxId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getParentTxId() {
    return parentTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public long getTimeout() {
    return timeout;
  }

  public long getReverseRetries() {
    return reverseRetries;
  }

  public String getCompensationMethod() {
    return compensationMethod;
  }

  public String getException() {
    return exception;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public void setReverseRetries(long reverseRetries) {
    this.reverseRetries = reverseRetries;
  }

  public long getForwardRetries() {
    return forwardRetries;
  }

  public void setForwardRetries(long forwardRetries) {
    this.forwardRetries = forwardRetries;
  }

  public long getReverseTimeout() {
    return reverseTimeout;
  }

  public void setReverseTimeout(long reverseTimeout) {
    this.reverseTimeout = reverseTimeout;
  }

  public long getForwardTimeout() {
    return forwardTimeout;
  }

  public void setForwardTimeout(long forwardTimeout) {
    this.forwardTimeout = forwardTimeout;
  }

  public long getRetryDelayInMilliseconds() {
    return retryDelayInMilliseconds;
  }

  public void setRetryDelayInMilliseconds(long retryDelayInMilliseconds) {
    this.retryDelayInMilliseconds = retryDelayInMilliseconds;
  }

  public void setCompensationMethod(String compensationMethod) {
    this.compensationMethod = compensationMethod;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public static Builder builder() {
    return new Builder();
  }


  public static final class Builder {

    private String type;
    private String globalTxId;
    private String serviceName;
    private String instanceId;
    private String parentTxId;
    private String localTxId;
    private Date createTime;
    private long timeout;
    private long reverseRetries;
    private long forwardRetries;
    private long reverseTimeout;
    private long forwardTimeout;
    private long retryDelayInMilliseconds;
    private String compensationMethod;
    private String exception;

    private Builder() {
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      this.globalTxId = globalTxId;
      return this;
    }

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder parentTxId(String parentTxId) {
      this.parentTxId = parentTxId;
      return this;
    }

    public Builder localTxId(String localTxId) {
      this.localTxId = localTxId;
      return this;
    }

    public Builder createTime(Date createTime) {
      this.createTime = createTime;
      return this;
    }

    public Builder timeout(long timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder reverseRetries(long reverseRetries) {
      this.reverseRetries = reverseRetries;
      return this;
    }

    public Builder forwardRetries(long forwardRetries) {
      this.forwardRetries = forwardRetries;
      return this;
    }

    public Builder reverseTimeout(long reverseTimeout) {
      this.reverseTimeout = reverseTimeout;
      return this;
    }

    public Builder forwardTimeout(long forwardTimeout) {
      this.forwardTimeout = forwardTimeout;
      return this;
    }

    public Builder retryDelayInMilliseconds(long retryDelayInMilliseconds) {
      this.retryDelayInMilliseconds = retryDelayInMilliseconds;
      return this;
    }

    public Builder compensationMethod(String compensationMethod) {
      this.compensationMethod = compensationMethod;
      return this;
    }

    public Builder exception(String exception) {
      this.exception = exception;
      return this;
    }

    public EventDTO build() {
      EventDTO eventDTO = new EventDTO();
      eventDTO.parentTxId = this.parentTxId;
      eventDTO.serviceName = this.serviceName;
      eventDTO.createTime = this.createTime;
      eventDTO.type = this.type;
      eventDTO.localTxId = this.localTxId;
      eventDTO.instanceId = this.instanceId;
      eventDTO.globalTxId = this.globalTxId;
      eventDTO.timeout = this.timeout;
      eventDTO.reverseRetries = this.reverseRetries;
      eventDTO.compensationMethod = this.compensationMethod;
      eventDTO.exception = this.exception;
      return eventDTO;
    }
  }
}
