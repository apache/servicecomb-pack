/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils.MethodCallback;

public abstract class MethodCheckingCallback implements MethodCallback {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Object bean;

  private final CallbackContext callbackContext;

  private final CallbackType callbackType;

  public MethodCheckingCallback(Object bean, CallbackContext callbackContext, CallbackType callbackType) {
    this.bean = bean;
    this.callbackContext = callbackContext;
    this.callbackType = callbackType;
  }

  protected void loadMethodContext(Method method, String ... candidates) {
    for (String each : candidates) {
      try {
        Method signature = bean.getClass().getDeclaredMethod(each, method.getParameterTypes());
        callbackContext.addCallbackContext(signature, bean);
        LOG.debug("Found callback method [{}] in {}", each, bean.getClass().getCanonicalName());
      } catch (NoSuchMethodException ex) {
        throw new OmegaException(
            "No such " + callbackType + " method [" + each + "] found in " + bean.getClass().getCanonicalName(), ex);
      }
    }
  }
}
