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

package org.apache.servicecomb.pack.omega.transaction.wrapper;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.SagaStartAnnotationProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SagaStartAnnotationProcessorWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final SagaStartAnnotationProcessor sagaStartAnnotationProcessor;

  public SagaStartAnnotationProcessorWrapper(
      SagaStartAnnotationProcessor sagaStartAnnotationProcessor) {
    this.sagaStartAnnotationProcessor = sagaStartAnnotationProcessor;
  }

  public Object apply(ProceedingJoinPoint joinPoint, SagaStart sagaStart, OmegaContext context)
      throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    sagaStartAnnotationProcessor.preIntercept(sagaStart.timeout());
    LOG.debug("Initialized context {} before execution of method {}", context, method.toString());
    try {
      Object result = joinPoint.proceed();
      if (sagaStart.autoClose()) {
        sagaStartAnnotationProcessor.postIntercept(context.globalTxId());
        LOG.debug("Transaction with context {} has finished.", context);
      } else {
        LOG.debug("Transaction with context {} is not finished in the SagaStarted annotated method.", context);
      }
      return result;
    } catch (Throwable throwable) {
      // We don't need to handle the OmegaException here
      if (!(throwable instanceof OmegaException)) {
        sagaStartAnnotationProcessor.onError(method.toString(), throwable);
        LOG.error("Transaction {} failed.", context.globalTxId());
      }
      throw throwable;
    } finally {
      context.clear();
    }
  }
}
