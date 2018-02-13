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

package org.apache.servicecomb.saga.alpha.core;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "TxTimeout")
public class TxTimeout {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long surrogateId;

  private long eventId;
  private String serviceName;
  private String instanceId;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String type;
  private Date expiryTime;
  private String status;

  @Version
  private long version;

  TxTimeout() {
  }

  TxTimeout(long eventId, String serviceName, String instanceId, String globalTxId, String localTxId,
      String parentTxId, String type, Date expiryTime, String status) {
    this.eventId = eventId;
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.type = type;
    this.expiryTime = expiryTime;
    this.status = status;
  }

  public String serviceName() {
    return serviceName;
  }

  public String instanceId() {
    return instanceId;
  }

  public String globalTxId() {
    return globalTxId;
  }

  public String localTxId() {
    return localTxId;
  }

  public String parentTxId() {
    return parentTxId;
  }

  public String type() {
    return type;
  }

  public Date expiryTime() {
    return expiryTime;
  }

  public String status() {
    return status;
  }

  @Override
  public String toString() {
    return "TxTimeout{" +
        "eventId=" + eventId +
        ", serviceName='" + serviceName + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", type='" + type + '\'' +
        ", expiryTime=" + expiryTime +
        ", status=" + status +
        '}';
  }
}
