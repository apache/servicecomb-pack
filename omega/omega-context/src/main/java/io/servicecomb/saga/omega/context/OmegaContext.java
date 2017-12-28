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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmegaContext {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String GLOBAL_TX_ID_KEY = "X-Pack-Global-Transaction-Id";
  public static final String LOCAL_TX_ID_KEY = "X-Pack-Local-Transaction-Id";

  private final ThreadLocal<String> globalTxId = new ThreadLocal<>();
  private final ThreadLocal<String> localTxId = new ThreadLocal<>();
  private final ThreadLocal<String> parentTxId = new ThreadLocal<>();
  private final Map<String, Map<String, CompensationContext>> compensationContexts = new ConcurrentHashMap<>();
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

  public String parentTxId() {
    return parentTxId.get();
  }

  public void setParentTxId(String parentTxId) {
    this.parentTxId.set(parentTxId);
  }

  // TODO: 2017/12/23 remove this context entry by the end of its corresponding global tx
  public void addContext(String globalTxId, String localTxId, Object target, String compensationMethod, Object... args) {
    compensationContexts.computeIfAbsent(globalTxId, k -> new ConcurrentHashMap<>())
        .put(localTxId, new CompensationContext(target, compensationMethod, args));
  }

  public boolean containsContext(String globalTxId) {
    return compensationContexts.containsKey(globalTxId);
  }

  public void compensate(String globalTxId) {
    Map<String, CompensationContext> contexts = compensationContexts.remove(globalTxId);

    for (CompensationContext compensationContext : contexts.values()) {
      try {
        invokeMethod(compensationContext);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        LOG.error(
            "Pre-checking for compensate method " + compensationContext.compensationMethod
                + " was somehow skipped, did you forget to configure compensable method checking on service startup?",
            e);
      }
    }
  }

  private void invokeMethod(CompensationContext compensationContext)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    Method method = compensationContext.target
        .getClass()
        .getDeclaredMethod(compensationContext.compensationMethod, argClasses(compensationContext));
    method.setAccessible(true);

    method.invoke(compensationContext.target, compensationContext.args);
  }

  private Class<?>[] argClasses(CompensationContext compensationContext) {
    Class<?>[] classes = new Class<?>[compensationContext.args.length];

    for (int i = 0; i < compensationContext.args.length; i++) {
      classes[i] = compensationContext.args[i].getClass();
    }

    return classes;
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
    private final String compensationMethod;
    private final Object[] args;

    private CompensationContext(Object target, String compensationMethod, Object... args) {
      this.target = target;
      this.compensationMethod = compensationMethod;
      this.args = args;
    }
  }
}
