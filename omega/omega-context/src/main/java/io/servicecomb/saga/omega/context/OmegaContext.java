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

package io.servicecomb.saga.omega.context;

public class OmegaContext {
  private final ThreadLocal<String> globalTxId = new ThreadLocal<>();
  private final ThreadLocal<String> localTxId = new ThreadLocal<>();
  private final ThreadLocal<String> parentTxId = new ThreadLocal<>();


  public void setGlobalTxId(String txId) {
    globalTxId.set(txId);
  }

  public String globalTxId() {
    return globalTxId.get();
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId.set(localTxId);
  }

  public String localTxId() {
    return localTxId.get();
  }

  public String parentTxId() {
    return parentTxId.get();
  }

  public void setParentTxId(String parentTxId) {
    this.parentTxId.set(parentTxId);
  }

  @Override
  public String toString() {
    return "OmegaContext{" +
        "globalTxId=" + globalTxId.get() +
        ", localTxId=" + localTxId.get() +
        ", parentTxId=" + parentTxId.get() +
        '}';
  }
}
