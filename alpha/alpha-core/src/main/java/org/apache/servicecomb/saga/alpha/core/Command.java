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

import static org.apache.servicecomb.saga.alpha.core.CommandStatus.NEW;

public class Command {
  private String serviceName;
  private String instanceId;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String compensationMethod;
  private byte[] payloads;
  private String status;

  Command() {
  }

  Command(String serviceName,
      String instanceId,
      String globalTxId,
      String localTxId,
      String parentTxId,
      String compensationMethod,
      byte[] payloads,
      String status) {

    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.compensationMethod = compensationMethod;
    this.payloads = payloads;
    this.status = status;
  }

  Command(String serviceName,
      String instanceId,
      String globalTxId,
      String localTxId,
      String parentTxId,
      String compensationMethod,
      byte[] payloads) {

    this(serviceName, instanceId, globalTxId, localTxId, parentTxId, compensationMethod, payloads, NEW.name());
  }

  Command(Command command, CommandStatus status) {
    this(command.serviceName,
        command.instanceId,
        command.globalTxId,
        command.localTxId,
        command.parentTxId,
        command.compensationMethod,
        command.payloads,
        status.name());
  }

  public Command(TxEvent event) {
    this(event.serviceName(),
        event.instanceId(),
        event.globalTxId(),
        event.localTxId(),
        event.parentTxId(),
        event.compensationMethod(),
        event.payloads());
  }

  String serviceName() {
    return serviceName;
  }

  String instanceId() {
    return instanceId;
  }

  String globalTxId() {
    return globalTxId;
  }

  String localTxId() {
    return localTxId;
  }

  String parentTxId() {
    return parentTxId;
  }

  String compensationMethod() {
    return compensationMethod;
  }

  byte[] payloads() {
    return payloads;
  }

  String status() {
    return status;
  }
}
