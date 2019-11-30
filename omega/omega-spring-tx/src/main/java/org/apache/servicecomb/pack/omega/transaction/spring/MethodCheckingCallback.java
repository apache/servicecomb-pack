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

package org.apache.servicecomb.pack.omega.transaction.spring;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.servicecomb.pack.omega.transaction.CallbackContext;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
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
        String key = getTargetBean(bean).getClass().getDeclaredMethod(each, method.getParameterTypes()).toString();
        callbackContext.addCallbackContext(key, signature, bean);
        LOG.debug("Found callback method [{}] in {}", each, bean.getClass().getCanonicalName());
      } catch (Exception ex) {
        throw new OmegaException(
            "No such " + callbackType + " method [" + each + "] found in " + bean.getClass().getCanonicalName(), ex);
      }
    }
  }

  private Object getTargetBean(Object proxy) throws Exception {
    if(!AopUtils.isAopProxy(proxy)) {
      return proxy;
    }

    if(AopUtils.isJdkDynamicProxy(proxy)) {
      return getJdkDynamicProxyTargetObject(proxy);
    } else {
      return getCglibProxyTargetObject(proxy);
    }
  }

  private Object getCglibProxyTargetObject(Object proxy) throws Exception {
    Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
    h.setAccessible(true);
    Object dynamicAdvisedInterceptor = h.get(proxy);

    Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
    advised.setAccessible(true);

    Object result = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
    return result;
  }


  private Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
    Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
    h.setAccessible(true);
    AopProxy aopProxy = (AopProxy) h.get(proxy);

    Field advised = aopProxy.getClass().getDeclaredField("advised");
    advised.setAccessible(true);

    Object result = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
    return result;
  }
}
