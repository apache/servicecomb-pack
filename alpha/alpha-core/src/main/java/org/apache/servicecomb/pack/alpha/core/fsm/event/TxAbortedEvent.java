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

public class TxAbortedEvent extends TxEvent {

  private byte[] payloads;

  public byte[] getPayloads() {
    return payloads;
  }

  public void setPayloads(byte[] payloads) {
    this.payloads = payloads;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private TxAbortedEvent txAbortedEvent;

    private Builder() {
      txAbortedEvent = new TxAbortedEvent();
    }

    public Builder serviceName(String serviceName) {
      txAbortedEvent.setServiceName(serviceName);
      return this;
    }

    public Builder instanceId(String instanceId) {
      txAbortedEvent.setInstanceId(instanceId);
      return this;
    }

    public Builder parentTxId(String parentTxId) {
      txAbortedEvent.setParentTxId(parentTxId);
      return this;
    }

    public Builder localTxId(String localTxId) {
      txAbortedEvent.setLocalTxId(localTxId);
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      txAbortedEvent.setGlobalTxId(globalTxId);
      return this;
    }

    public Builder payloads(byte[] payloads) {
      txAbortedEvent.setPayloads(payloads);
      return this;
    }

    public Builder createTime(Date createTime){
      txAbortedEvent.setCreateTime(createTime);
      return this;
    }

    public TxAbortedEvent build() {
      return txAbortedEvent;
    }
  }
}
