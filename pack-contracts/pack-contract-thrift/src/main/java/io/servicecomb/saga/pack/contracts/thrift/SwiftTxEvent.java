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

package io.servicecomb.saga.pack.contracts.thrift;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct("TxEvent")
public class SwiftTxEvent {
  private final long timestamp;
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
  private final String type;
  private final byte[] payloads;

  @ThriftConstructor
  public SwiftTxEvent(long timestamp, String globalTxId, String localTxId, String parentTxId, String type, byte[] payloads) {
    this.timestamp = timestamp;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.type = type;
    this.payloads = payloads;
  }

  @ThriftField(1)
  public long timestamp() {
    return timestamp;
  }

  @ThriftField(2)
  public String globalTxId() {
    return globalTxId;
  }

  @ThriftField(3)
  public String localTxId() {
    return localTxId;
  }

  @ThriftField(4)
  public String parentTxId() {
    return parentTxId;
  }

  @ThriftField(5)
  public String type() {
    return type;
  }

  @ThriftField(6)
  public byte[] payloads() {
    return payloads;
  }
}
