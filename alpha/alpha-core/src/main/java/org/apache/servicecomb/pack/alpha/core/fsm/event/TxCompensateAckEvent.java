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

public class TxCompensateAckEvent extends TxEvent {

  private boolean succeed;

  public boolean isSucceed() {
    return succeed;
  }

  public void setSucceed(boolean succeed) {
    this.succeed = succeed;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxCompensateAckEvent txCompensatedEvent;

    private Builder() {
      txCompensatedEvent = new TxCompensateAckEvent();
    }

    public Builder serviceName(String serviceName) {
      txCompensatedEvent.setServiceName(serviceName);
      return this;
    }

    public Builder instanceId(String instanceId) {
      txCompensatedEvent.setInstanceId(instanceId);
      return this;
    }

    public Builder parentTxId(String parentTxId) {
      txCompensatedEvent.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txCompensatedEvent.setLocalTxId(localTxId);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txCompensatedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public Builder createTime(Date createTime){
      txCompensatedEvent.setCreateTime(createTime);
      return this;
    }

    public Builder succeed(boolean succeed){
      txCompensatedEvent.setSucceed(succeed);
      return this;
    }

    public TxCompensateAckEvent build() {
      return txCompensatedEvent;
    }
  }
}