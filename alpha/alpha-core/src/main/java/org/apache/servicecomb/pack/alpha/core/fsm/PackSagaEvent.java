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

package org.apache.servicecomb.pack.alpha.core.fsm;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "PACK_SAGA_EVENT")
public class PackSagaEvent {
  @Transient
  public static final long MAX_TIMESTAMP = 253402214400000L; // 9999-12-31 00:00:00

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long surrogateId;

  private String serviceName;
  private String instanceId;
  private Date creationTime;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String type;
  private String compensationMethod;
  private Date expiryTime;
  private String retryMethod;
  private int retries;
  private byte[] payloads;

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String serviceName;
    private String instanceId;
    private Date creationTime;
    private String globalTxId;
    private String localTxId;
    private String parentTxId;
    private String type;
    private String compensationMethod;
    private Date expiryTime;
    private String retryMethod;
    private int retries;
    private byte[] payloads;

    private Builder() {
    }

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder creationTime(Date creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public Builder globalTxId(String globalTxId) {
      this.globalTxId = globalTxId;
      return this;
    }

    public Builder localTxId(String localTxId) {
      this.localTxId = localTxId;
      return this;
    }

    public Builder parentTxId(String parentTxId) {
      this.parentTxId = parentTxId;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder compensationMethod(String compensationMethod) {
      this.compensationMethod = compensationMethod;
      return this;
    }

    public Builder expiryTime(Date expiryTime) {
      this.expiryTime = expiryTime;
      return this;
    }

    public Builder retryMethod(String retryMethod) {
      this.retryMethod = retryMethod;
      return this;
    }

    public Builder retries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder payloads(byte[] payloads) {
      this.payloads = payloads;
      return this;
    }

    public PackSagaEvent build() {
      PackSagaEvent packSagaEvent = new PackSagaEvent();
      packSagaEvent.instanceId = this.instanceId;
      packSagaEvent.serviceName = this.serviceName;
      packSagaEvent.localTxId = this.localTxId;
      packSagaEvent.retryMethod = this.retryMethod;
      packSagaEvent.creationTime = this.creationTime;
      packSagaEvent.compensationMethod = this.compensationMethod;
      packSagaEvent.payloads = this.payloads;
      packSagaEvent.globalTxId = this.globalTxId;
      packSagaEvent.retries = this.retries;
      packSagaEvent.type = this.type;
      packSagaEvent.parentTxId = this.parentTxId;
      packSagaEvent.expiryTime = this.expiryTime;
      return packSagaEvent;
    }
  }
}
