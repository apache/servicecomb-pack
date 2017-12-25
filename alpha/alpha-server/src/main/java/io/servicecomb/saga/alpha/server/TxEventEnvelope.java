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

package io.servicecomb.saga.alpha.server;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.servicecomb.saga.alpha.core.TxEvent;

@Entity
class TxEventEnvelope {
  @Id
  @GeneratedValue
  private long surrogateId;

  @Embedded
  private TxEvent event;

  private TxEventEnvelope() {
  }

  TxEventEnvelope(TxEvent event) {
    this.event = event;
  }

  public long timestamp() {
    return event.timestamp();
  }

  String globalTxId() {
    return event.globalTxId();
  }

  String localTxId() {
    return event.localTxId();
  }

  String parentTxId() {
    return event.parentTxId();
  }

  String type() {
    return event.type();
  }

  byte[] payloads() {
    return event.payloads();
  }
}
