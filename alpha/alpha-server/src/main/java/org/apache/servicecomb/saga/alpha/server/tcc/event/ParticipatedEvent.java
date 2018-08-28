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

package org.apache.servicecomb.saga.alpha.server.tcc.event;

import org.apache.servicecomb.saga.common.TransactionStatus;

public class ParticipatedEvent {

  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String serviceName;
  private String instanceId;
  private String confirmMethod;
  private String cancelMethod;
  private TransactionStatus status;

  public ParticipatedEvent(String globalTxId, String localTxId, String parentTxId, String serviceName,
      String instanceId, String confirmMethod, String cancelMethod, TransactionStatus status) {
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.confirmMethod = confirmMethod;
    this.cancelMethod = cancelMethod;
    this.status = status;
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

  public TransactionStatus getStatus() {
    return status;
  }
}
