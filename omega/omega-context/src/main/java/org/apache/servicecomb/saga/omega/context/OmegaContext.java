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

package org.apache.servicecomb.saga.omega.context;

/**
 * OmegaContext holds the globalTxId and localTxId which are used to build the invocation map
 */
public class OmegaContext {
  public static final String GLOBAL_TX_ID_KEY = "X-Pack-Global-Transaction-Id";
  public static final String LOCAL_TX_ID_KEY = "X-Pack-Local-Transaction-Id";

  private final ThreadLocal<String> globalTxId = new InheritableThreadLocal<>();
  private final ThreadLocal<String> localTxId = new InheritableThreadLocal<>();
  private final IdGenerator<String> idGenerator;

  public OmegaContext(IdGenerator<String> idGenerator) {
    this.idGenerator = idGenerator;
  }

  public String newGlobalTxId() {
    String id = idGenerator.nextId();
    globalTxId.set(id);
    return id;
  }

  public void setGlobalTxId(String txId) {
    globalTxId.set(txId);
  }

  public String globalTxId() {
    return globalTxId.get();
  }

  public String newLocalTxId() {
    String id = idGenerator.nextId();
    localTxId.set(id);
    return id;
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId.set(localTxId);
  }

  public String localTxId() {
    return localTxId.get();
  }

  public void clear() {
    globalTxId.remove();
    localTxId.remove();
  }

  @Override
  public String toString() {
    return "OmegaContext{" +
        "globalTxId=" + globalTxId.get() +
        ", localTxId=" + localTxId.get() +
        '}';
  }
}
