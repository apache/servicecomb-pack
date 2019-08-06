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
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.SagaEvent;

public class SagaStartedEvent extends SagaEvent {
  private int timeout; //second

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return "SagaStartedEvent{" +
        "timeout=" + timeout +
        "} " + super.toString();
  }

  public static final class Builder {

    private SagaStartedEvent sagaStartedEvent;

    private Builder() {
      sagaStartedEvent = new SagaStartedEvent();
    }

    public Builder globalTxId(String globalTxId) {
      sagaStartedEvent.setGlobalTxId(globalTxId);
      sagaStartedEvent.setLocalTxId(globalTxId);
      sagaStartedEvent.setParentTxId(globalTxId);
      return this;
    }

    public Builder timeout(int timeout) {
      sagaStartedEvent.setTimeout(timeout);
      return this;
    }

    public Builder serviceName(String serviceName) {
      sagaStartedEvent.setServiceName(serviceName);
      return this;
    }

    public Builder instanceId(String instanceId) {
      sagaStartedEvent.setInstanceId(instanceId);
      return this;
    }

    public Builder createTime(Date createTime){
      sagaStartedEvent.setCreateTime(createTime);
      return this;
    }

    public SagaStartedEvent build() {
      return sagaStartedEvent;
    }
  }
}
