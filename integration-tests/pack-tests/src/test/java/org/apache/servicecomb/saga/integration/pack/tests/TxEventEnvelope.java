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

package org.apache.servicecomb.saga.integration.pack.tests;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
class TxEventEnvelope {
  @Id
  @GeneratedValue
  private long surrogateId;

  private String serviceName;
  private String instanceId;
  private Date creationTime;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String type;
  private byte[] payloads;

  private TxEventEnvelope() {
  }

  String serviceName() {
    return serviceName;
  }

  String instanceId() {
    return instanceId;
  }

  String localTxId() {
    return localTxId;
  }

  String parentTxId() {
    return parentTxId;
  }

  String type() {
    return type;
  }

  public byte[] payloads() {
    return payloads;
  }

  @Override
  public String toString() {
    return "TxEventEnvelope{" +
        "surrogateId=" + surrogateId +
        ", serviceName='" + serviceName + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", creationTime=" + creationTime +
        ", globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", type='" + type + '\'' +
        ", payloads=" + Arrays.toString(payloads) +
        '}';
  }
}
