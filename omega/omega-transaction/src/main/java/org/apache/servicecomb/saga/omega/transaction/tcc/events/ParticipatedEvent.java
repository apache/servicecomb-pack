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
package org.apache.servicecomb.saga.omega.transaction.tcc.events;


import org.apache.servicecomb.saga.common.TransactionStatus;

public class ParticipatedEvent {
  
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
  private final String confirmMethod;
  private final String cancelMethod;
  private final TransactionStatus status;


  public ParticipatedEvent(String globalTxId, String localTxId, String parentTxId, String confirmMethod,
      String cancelMethod, TransactionStatus status) {
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
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

  public String getConfirmMethod() {
    return confirmMethod;
  }

  public String getCancelMethod() {
    return cancelMethod;
  }

  public TransactionStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "ParticipatedEvent{" +
        "globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", confirmMethod='" + confirmMethod + '\'' +
        ", cancelMethod='" + cancelMethod + '\'' +
        ", status=" + status +
        '}';
  }
}
