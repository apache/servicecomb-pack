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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompensationContext {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, CompensationContextInternal> contexts = new ConcurrentHashMap<>();

  public void addCompensationContext(Method compensationMethod, Object target) {
    compensationMethod.setAccessible(true);
    contexts.put(compensationMethod.toString(), new CompensationContextInternal(target, compensationMethod));
  }

  public void apply(String globalTxId, String localTxId, String compensationMethod, Object... payloads) {
    CompensationContextInternal contextInternal = contexts.get(compensationMethod);

    try {
      contextInternal.compensationMethod.invoke(contextInternal.target, payloads);
      LOG.info("Compensated transaction with global tx id [{}], local tx id [{}]", globalTxId, localTxId);
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOG.error(
          "Pre-checking for compensation method " + contextInternal.compensationMethod.toString()
              + " was somehow skipped, did you forget to configure compensable method checking on service startup?",
          e);
    }
  }

  private static final class CompensationContextInternal {
    private final Object target;

    private final Method compensationMethod;

    private CompensationContextInternal(Object target, Method compensationMethod) {
      this.target = target;
      this.compensationMethod = compensationMethod;
    }
  }
}
