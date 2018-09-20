/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server.tcc.jpa;

import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "tcc_participate_event")
public class ParticipatedEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long surrogateId;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String serviceName;
  private String instanceId;
  private String confirmMethod;
  private String cancelMethod;
  private String status;
  private Date creationTime;
  private Date lastModified;

  private ParticipatedEvent() {
  }

  public ParticipatedEvent(String serviceName, String instanceId, String globalTxId, String localTxId,
      String parentTxId,  String confirmMethod, String cancelMethod, String status, Date creationTime, Date lastModified) {
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.confirmMethod = confirmMethod;
    this.cancelMethod = cancelMethod;
    this.status = status;
    this.creationTime = creationTime;
    this.lastModified = lastModified;
  }

  public ParticipatedEvent(String serviceName, String instanceId, String globalTxId, String localTxId,
      String parentTxId,  String confirmMethod, String cancelMethod, String status) {
    this(serviceName, instanceId, globalTxId, localTxId, parentTxId, confirmMethod, cancelMethod, status, new Date(), new Date());
  }

  public Long getId() {
    return surrogateId;
  }

  public String getGlobalTxId() {
    return globalTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public String getParentTxId() {
    return parentTxId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getConfirmMethod() {
    return confirmMethod;
  }

  public String getCancelMethod() {
    return cancelMethod;
  }

  public String getStatus() {
    return status;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public Date getLastModified() {
    return lastModified;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipatedEvent that = (ParticipatedEvent) o;
    return Objects.equals(globalTxId, that.globalTxId) &&
        Objects.equals(localTxId, that.localTxId) &&
        Objects.equals(parentTxId, that.parentTxId) &&
        Objects.equals(serviceName, that.serviceName) &&
        Objects.equals(instanceId, that.instanceId) &&
        Objects.equals(confirmMethod, that.confirmMethod) &&
        Objects.equals(cancelMethod, that.cancelMethod) &&
        Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(globalTxId, localTxId, parentTxId, serviceName, instanceId, confirmMethod, cancelMethod, status);
  }

  @Override
  public String toString() {
    return "ParticipatedEvent{" +
        "surrogateId=" + surrogateId +
        ", globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", serviceName='" + serviceName + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", confirmMethod='" + confirmMethod + '\'' +
        ", cancelMethod='" + cancelMethod + '\'' +
        ", status='" + status + '\'' +
        ", creationTime=" + creationTime +
        ", lastModified=" + lastModified +
        '}';
  }
}
