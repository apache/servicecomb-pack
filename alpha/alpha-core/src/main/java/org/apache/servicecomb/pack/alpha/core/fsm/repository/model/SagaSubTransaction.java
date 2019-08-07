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

package org.apache.servicecomb.pack.alpha.core.fsm.repository.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;

public class SagaSubTransaction {
  private String localTxId;
  private String parentTxId;
  @JsonFormat(shape = JsonFormat.Shape.NUMBER)
  private Date beginTime;
  @JsonFormat(shape = JsonFormat.Shape.NUMBER)
  private Date endTime;
  private TxState state;
  private Long durationTime;

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

  public TxState getState() {
    return state;
  }

  public Long getDurationTime() {
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
    private TxState state;

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

    public Builder state(TxState state) {
      this.state = state;
      return this;
    }

    public SagaSubTransaction build() {
      SagaSubTransaction sagaSubTransaction = new SagaSubTransaction();
      sagaSubTransaction.parentTxId = this.parentTxId;
      sagaSubTransaction.beginTime = this.beginTime;
      sagaSubTransaction.state = this.state;
      sagaSubTransaction.durationTime = this.endTime.getTime() - this.beginTime.getTime();
      sagaSubTransaction.localTxId = this.localTxId;
      sagaSubTransaction.endTime = this.endTime;
      return sagaSubTransaction;
    }
  }
}
