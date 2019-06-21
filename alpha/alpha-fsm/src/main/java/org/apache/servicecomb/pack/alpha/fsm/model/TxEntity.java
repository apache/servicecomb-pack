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
import org.apache.servicecomb.pack.alpha.fsm.TxState;

public class TxEntity implements Serializable {
  private long beginTime = System.currentTimeMillis();
  private long endTime;
  private String parentTxId;
  private String localTxId;
  private TxState state;

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

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxEntity txEntity;

    private Builder() {
      txEntity = new TxEntity();
    }

    public Builder beginTime(long beginTime) {
      txEntity.setBeginTime(beginTime);
      return this;
    }

    public Builder endTime(long endTime) {
      txEntity.setEndTime(endTime);
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

    public Builder state(TxState state) {
      txEntity.setState(state);
      return this;
    }

    public TxEntity build() {
      return txEntity;
    }
  }
}
