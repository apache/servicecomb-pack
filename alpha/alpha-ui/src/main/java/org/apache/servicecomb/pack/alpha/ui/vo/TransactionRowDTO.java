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

public class TransactionRowDTO {

  private String globalTxId;
  private String serviceName;
  private String instanceId;
  private Date beginTime;
  private Date endTime;
  private String state;
  private int subTxSize;
  private long durationTime;

  public String getGlobalTxId() {
    return globalTxId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public Date getBeginTime() {
    return beginTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public String getState() {
    return state;
  }

  public int getSubTxSize() {
    return subTxSize;
  }

  public long getDurationTime() {
    return durationTime;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String globalTxId;
    private String serviceName;
    private String instanceId;
    private Date beginTime;
    private Date endTime;
    private String state;
    private int subTxSize;
    private long durationTime;

    private Builder() {
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

    public Builder beginTime(Date beginTime) {
      this.beginTime = beginTime;
      return this;
    }

    public Builder endTime(Date endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder state(String state) {
      this.state = state;
      return this;
    }

    public Builder subTxSize(int subTxSize) {
      this.subTxSize = subTxSize;
      return this;
    }

    public Builder durationTime(long durationTime) {
      this.durationTime = durationTime;
      return this;
    }

    public TransactionRowDTO build() {
      TransactionRowDTO transactionRowDTO = new TransactionRowDTO();
      transactionRowDTO.state = this.state;
      transactionRowDTO.instanceId = this.instanceId;
      transactionRowDTO.endTime = this.endTime;
      transactionRowDTO.subTxSize = this.subTxSize;
      transactionRowDTO.durationTime = this.durationTime;
      transactionRowDTO.globalTxId = this.globalTxId;
      transactionRowDTO.serviceName = this.serviceName;
      transactionRowDTO.beginTime = this.beginTime;
      return transactionRowDTO;
    }
  }
}
