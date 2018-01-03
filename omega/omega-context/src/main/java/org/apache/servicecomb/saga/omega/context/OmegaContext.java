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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmegaContext {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String GLOBAL_TX_ID_KEY = "X-Pack-Global-Transaction-Id";
  public static final String LOCAL_TX_ID_KEY = "X-Pack-Local-Transaction-Id";

  private final ThreadLocal<String> globalTxId = new InheritableThreadLocal<>();
  private final ThreadLocal<String> localTxId = new InheritableThreadLocal<>();
  private final ThreadLocal<String> parentTxId = new InheritableThreadLocal<>();
  private final IdGenerator<String> idGenerator;
  private final Map<String, CompensationContext> compensationContexts = new HashMap<>();

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

  public String parentTxId() {
    return parentTxId.get();
  }

  public void setParentTxId(String parentTxId) {
    this.parentTxId.set(parentTxId);
  }

  public void clear() {
    globalTxId.remove();
    localTxId.remove();
    parentTxId.remove();
  }

  public void addCompensationContext(Method compensationMethod, Object target) {
    compensationMethod.setAccessible(true);
    compensationContexts.put(compensationMethod.toString(), new CompensationContext(target, compensationMethod));
  }

  public void compensate(String globalTxId, String localTxId, String compensationMethod, Object... payloads) {
    CompensationContext compensationContext = compensationContexts.get(compensationMethod);

    try {
      compensationContext.compensationMethod.invoke(compensationContext.target, payloads);
      LOG.info("Compensated transaction with global tx id [{}], local tx id [{}]", globalTxId, localTxId);
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOG.error(
          "Pre-checking for compensate method " + compensationContext.compensationMethod.toString()
              + " was somehow skipped, did you forget to configure compensable method checking on service startup?",
          e);
    }
  }

  @Override
  public String toString() {
    return "OmegaContext{" +
        "globalTxId=" + globalTxId.get() +
        ", localTxId=" + localTxId.get() +
        ", parentTxId=" + parentTxId.get() +
        '}';
  }

  private static final class CompensationContext {
    private final Object target;
    private final Method compensationMethod;

    private CompensationContext(Object target, Method compensationMethod) {
      this.target = target;
      this.compensationMethod = compensationMethod;
    }
  }
}
