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

package org.apache.servicecomb.saga.omega.transaction;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.transaction.InvalidTransactionException;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class TransactionAspect {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final OmegaContext context;
  private final EventAwareInterceptor interceptor;
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  public TransactionAspect(MessageSender messageSender, OmegaContext context) {
    this.context = context;
    this.interceptor = new CompensableInterceptor(context,messageSender);
  }

  @Around("execution(@org.apache.servicecomb.saga.omega.transaction.annotations.Compensable * *(..)) && @annotation(compensable)")
  Object advise(ProceedingJoinPoint joinPoint, Compensable compensable) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    LOG.debug("Intercepting compensable method {} with context {}", method.toString(), context);

    Object[] args = joinPoint.getArgs();
    int retries = compensable.retries();
    String retriesSignature = ((MethodSignature) joinPoint.getSignature()).getMethod().toString();
    String compensationSignature = compensationMethodSignature(joinPoint, compensable, method);

    String localTxId = context.localTxId();
    context.newLocalTxId();

    TimeAwareInterceptor interceptor = new TimeAwareInterceptor(this.interceptor);
    AlphaResponse response = interceptor.preIntercept(localTxId, retriesSignature, compensationSignature, retries, args);
    if (response.aborted()) {
      String abortedLocalTxId = context.localTxId();
      context.setLocalTxId(localTxId);
      throw new InvalidTransactionException("Abort sub transaction " + abortedLocalTxId +
          " because global transaction " + context.globalTxId() + " has already aborted.");
    }
    LOG.debug("Updated context {} for compensable method {} ", context, method.toString());

    scheduleTimeoutTask(interceptor, localTxId, compensationSignature, method, compensable.timeout());

    try {
      Object result = joinPoint.proceed();
      interceptor.postIntercept(localTxId, compensationSignature);

      return result;
    } catch (Throwable throwable) {
      interceptor.onError(localTxId, compensationSignature, throwable);
      throw throwable;
    } finally {
      context.setLocalTxId(localTxId);
      LOG.debug("Restored context back to {}", context);
    }
  }

  private void scheduleTimeoutTask(
      TimeAwareInterceptor interceptor,
      String parentTxId,
      String signature,
      Method method,
      int timeout) {

    if (timeout > 0) {
      executor.schedule(
          () -> interceptor.onTimeout(
              parentTxId,
              signature,
              new OmegaTxTimeoutException("Transaction " + method.toString() + " timed out")),
          timeout,
          MILLISECONDS);
    }
  }

  private String compensationMethodSignature(ProceedingJoinPoint joinPoint, Compensable compensable, Method method)
      throws NoSuchMethodException {

    return joinPoint.getTarget()
        .getClass()
        .getDeclaredMethod(compensable.compensationMethod(), method.getParameterTypes())
        .toString();
  }
}
