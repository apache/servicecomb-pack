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

package org.apache.servicecomb.pack.omega.transaction.spring;

import java.lang.reflect.Method;
import org.apache.servicecomb.pack.omega.transaction.CallbackContext;
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;

class CompensableMethodCheckingCallback extends MethodCheckingCallback {

  public CompensableMethodCheckingCallback(Object bean, CallbackContext callbackContext) {
    super(bean, callbackContext, CallbackType.Compensation);
  }

  @Override
  public void doWith(Method method) throws IllegalArgumentException {
    if (!method.isAnnotationPresent(Compensable.class)) {
      return;
    }
    Compensable compensable = method.getAnnotation(Compensable.class);
    String compensationMethod = compensable.compensationMethod();
    // we don't support the retries number below -1.
    if (compensable.forwardRetries() < -1) {
      throw new IllegalArgumentException(String.format("Compensable %s of method %s, the forward retries should not below -1.", compensable, method.getName()));
    }
    loadMethodContext(method, compensationMethod);
  }
}
