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

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.EventType;

import java.util.Arrays;

public class TxEvent {

  private final long timestamp;
  private final EventType type;
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
  private final String compensationMethod;
  private final Object[] payloads;
  private final String retriesMethod;
  private final int retries;

  public TxEvent(EventType type, String globalTxId, String localTxId, String parentTxId, String compensationMethod,
      Object... payloads) {
    this(type, globalTxId, localTxId, parentTxId, compensationMethod, null, 0, payloads);
  }

  public TxEvent(EventType type, String globalTxId, String localTxId, String parentTxId, String compensationMethod,
      String retriesMethod, int retries, Object... payloads) {
    this.timestamp = System.currentTimeMillis();
    this.type = type;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.compensationMethod = compensationMethod;
    this.payloads = payloads;
    this.globalTxId = globalTxId;
    this.retriesMethod = retriesMethod;
    this.retries = retries;
  }

  public long timestamp() {
    return timestamp;
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

  public Object[] payloads() {
    return payloads;
  }

  public EventType type() {
    return type;
  }

  public String compensationMethod() {
    return compensationMethod;
  }

  public String retriesMethod() {
    return retriesMethod;
  }

  public int retries() {
    return retries;
  }

  @Override
  public String toString() {
    return type.name() + "{" +
        "globalTxId='" + globalTxId + '\'' +
        ", localTxId='" + localTxId + '\'' +
        ", parentTxId='" + parentTxId + '\'' +
        ", compensationMethod='" + compensationMethod + '\'' +
        ", payloads=" + Arrays.toString(payloads) +
        '}';
  }
}
