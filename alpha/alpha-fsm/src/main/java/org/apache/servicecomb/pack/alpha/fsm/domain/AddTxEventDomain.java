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

package org.apache.servicecomb.pack.alpha.fsm.domain;

import org.apache.servicecomb.pack.alpha.fsm.TxState;

public class AddTxEventDomain implements DomainEvent {
  private String serviceName;
  private String instanceId;
  private String parentTxId;
  private String localTxId;
  private TxState state = TxState.ACTIVE;
  private int retries;
  private String compensationMethod;
  private byte[] payloads;

  public AddTxEventDomain(String serviceName, String instanceId, String parentTxId, String localTxId, byte[] payloads, String compensationMethod, int retries) {
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.parentTxId = parentTxId;
    this.localTxId = localTxId;
    this.compensationMethod = compensationMethod;
    this.payloads = payloads;
    this.retries = retries;
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

  public String getCompensationMethod() {
    return compensationMethod;
  }

  public void setCompensationMethod(String compensationMethod) {
    this.compensationMethod = compensationMethod;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public byte[] getPayloads() {
    return payloads;
  }

  public void setPayloads(byte[] payloads) {
    this.payloads = payloads;
  }
}
