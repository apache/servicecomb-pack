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

public class SubTransactionDTO {
  private String localTxId;
  private String parentTxId;
  private Date beginTime;
  private Date endTime;
  private String state;
  private long durationTime;

  public String getLocalTxId() {
    return localTxId;
  }

  public String getParentTxId() {
    return parentTxId;
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

  public long getDurationTime() {
    return durationTime;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String localTxId;
    private String parentTxId;
    private Date beginTime;
    private Date endTime;
    private String state;
    private long durationTime;

    private Builder() {
    }

    public Builder localTxId(String localTxId) {
      this.localTxId = localTxId;
      return this;
    }

    public Builder parentTxId(String parentTxId) {
      this.parentTxId = parentTxId;
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

    public Builder durationTime(long durationTime) {
      this.durationTime = durationTime;
      return this;
    }

    public SubTransactionDTO build() {
      SubTransactionDTO subTransactionDTO = new SubTransactionDTO();
      subTransactionDTO.localTxId = this.localTxId;
      subTransactionDTO.state = this.state;
      subTransactionDTO.durationTime = this.durationTime;
      subTransactionDTO.beginTime = this.beginTime;
      subTransactionDTO.endTime = this.endTime;
      subTransactionDTO.parentTxId = this.parentTxId;
      return subTransactionDTO;
    }
  }
}
