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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class SagaStartAspect {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final SagaStartAnnotationProcessor sagaStartAnnotationProcessor;

  private final OmegaContext context;

  public SagaStartAspect(MessageSender sender, OmegaContext context) {
    this.context = context;
    this.sagaStartAnnotationProcessor = new SagaStartAnnotationProcessor(context, sender);
  }

  @Around("execution(@org.apache.servicecomb.saga.omega.context.annotations.SagaStart * *(..)) && @annotation(sagaStart)")
  Object advise(ProceedingJoinPoint joinPoint, SagaStart sagaStart) throws Throwable {
    initializeOmegaContext();
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

    sagaStartAnnotationProcessor.preIntercept(context.globalTxId(), method.toString(), sagaStart.timeout(), "", 0);
    LOG.debug("Initialized context {} before execution of method {}", context, method.toString());

    try {
      Object result = joinPoint.proceed();

      sagaStartAnnotationProcessor.postIntercept(context.globalTxId(), method.toString());
      LOG.debug("Transaction with context {} has finished.", context);

      return result;
    } catch (Throwable throwable) {
      LOG.error("Transaction {} failed.", context.globalTxId());
      throw throwable;
    } finally {
      context.clear();
    }
  }

  private void initializeOmegaContext() {
    context.setLocalTxId(context.newGlobalTxId());
  }
}
