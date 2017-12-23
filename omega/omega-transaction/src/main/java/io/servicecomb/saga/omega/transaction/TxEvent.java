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

package io.servicecomb.saga.omega.transaction;

public abstract class TxEvent {
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
  private final Object[] payloads;

  TxEvent(String globalTxId, String localTxId, String parentTxId, Object[] payloads) {
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.payloads = payloads;
    this.globalTxId = globalTxId;
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

  public abstract String type();
}
