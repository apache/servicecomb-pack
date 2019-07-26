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

package org.apache.servicecomb.pack.alpha.fsm.event.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public abstract class BaseEvent implements Serializable {
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
    return this.getClass().getSimpleName()+"{" +
        "serviceName='" + serviceName + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", globalTxId='" + globalTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", createTime=" + createTime +
        '}';
  }

  public Map<String,Object> toMap() throws Exception {
    return mapper.readValue(mapper.writeValueAsString(this), Map.class);
  }
}
