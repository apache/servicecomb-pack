/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transaction;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import javax.transaction.InvalidTransactionException;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultRecovery is used to execute business logic once.
 * The corresponding events will report to alpha server before and after the execution of business logic.
 * If there are errors while executing the business logic, a TxAbortedEvent will be reported to alpha.
 *
 *                 pre                       post
 *     request --------- 2.business logic --------- response
 *                 \                          |
 * 1.TxStartedEvent \                        | 3.TxEndedEvent
 *                   \                      |
 *                    ----------------------
 *                            alpha
 */
public class DefaultRecovery implements RecoveryPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Object apply(ProceedingJoinPoint joinPoint, Compensable compensable, CompensableInterceptor interceptor,
      OmegaContext context, String parentTxId, int retries) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    LOG.debug("Intercepting compensable method {} with context {}", method.toString(), context);

    String compensationSignature =
        compensable.compensationMethod().isEmpty() ? "" : compensationMethodSignature(joinPoint, compensable, method);

    String retrySignature = (retries != 0 || compensationSignature.isEmpty()) ? method.toString() : "";

    AlphaResponse response = interceptor.preIntercept(parentTxId, compensationSignature, compensable.timeout(),
        retrySignature, retries, joinPoint.getArgs());
    if (response.aborted()) {
      String abortedLocalTxId = context.localTxId();
      context.setLocalTxId(parentTxId);
      throw new InvalidTransactionException("Abort sub transaction " + abortedLocalTxId +
          " because global transaction " + context.globalTxId() + " has already aborted.");
    }

    try {
      Object result = joinPoint.proceed();
      interceptor.postIntercept(parentTxId, compensationSignature);

      return result;
    } catch (Throwable throwable) {
      interceptor.onError(parentTxId, compensationSignature, throwable);
      throw throwable;
    }
  }

  String compensationMethodSignature(ProceedingJoinPoint joinPoint, Compensable compensable, Method method)
      throws NoSuchMethodException {
    return joinPoint.getTarget()
        .getClass()
        .getDeclaredMethod(compensable.compensationMethod(), method.getParameterTypes())
        .toString();
  }
}
