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

package org.apache.servicecomb.pack.alpha.core.fsm.event.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaTimeoutEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensatedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SagaStartedEvent.class, name = "SagaStartedEvent"),
    @JsonSubTypes.Type(value = SagaEndedEvent.class, name = "SagaEndedEvent"),
    @JsonSubTypes.Type(value = SagaAbortedEvent.class, name = "SagaAbortedEvent"),
    @JsonSubTypes.Type(value = SagaTimeoutEvent.class, name = "SagaTimeoutEvent"),
    @JsonSubTypes.Type(value = TxStartedEvent.class, name = "TxStartedEvent"),
    @JsonSubTypes.Type(value = TxEndedEvent.class, name = "TxEndedEvent"),
    @JsonSubTypes.Type(value = TxAbortedEvent.class, name = "TxAbortedEvent"),
    @JsonSubTypes.Type(value = TxCompensatedEvent.class, name = "TxCompensatedEvent")
})
public abstract class BaseEvent implements Serializable {

  private static final long serialVersionUID = 7587021626678201246L;
  private final ObjectMapper mapper = new ObjectMapper();
  private String serviceName;
  private String instanceId;
  private String globalTxId;
  private String parentTxId;
  private String localTxId;
  private Date createTime = new Date();

  public BaseEvent() {

  }

  public String getType() {
    return this.getClass().getSimpleName();
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public String getGlobalTxId() {
    return globalTxId;
  }

  public void setGlobalTxId(String globalTxId) {
    this.globalTxId = globalTxId;
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

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  @Override
  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String,Object> toMap() throws Exception {
    return mapper.readValue(mapper.writeValueAsString(this), Map.class);
  }
}
