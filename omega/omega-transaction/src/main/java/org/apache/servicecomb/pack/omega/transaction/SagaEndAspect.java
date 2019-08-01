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
package org.apache.servicecomb.pack.omega.transaction;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.annotations.SagaEnd;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Aspect
@Order(value = 300)
public class SagaEndAspect {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final OmegaContext context;
  private final SagaMessageSender sender;

  public SagaEndAspect(SagaMessageSender sender, OmegaContext context) {
    this.sender = sender;
    this.context = context;
  }

  @Around("execution(@org.apache.servicecomb.pack.omega.context.annotations.SagaEnded * *(..)) && @annotation(sagaEnd)")
  Object advise(ProceedingJoinPoint joinPoint, SagaEnd sagaEnd) throws Throwable {
      Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
      try {
        Object result = joinPoint.proceed();
        sendSagaEndedEvent();
        return result;
      } catch (Throwable throwable) {
        // Don't check the OmegaException here.
        if (!(throwable instanceof OmegaException)) {
          LOG.error("Transaction {} failed.", context.globalTxId());
          sendSagaAbortedEvent(method.toString(), throwable);
        }
        throw throwable;
      }
      finally {
        context.clear();
      }
  }

  private void sendSagaEndedEvent() {
    // TODO need to check the parentID setting
    sender.send(new SagaEndedEvent(context.globalTxId(), context.localTxId()));
  }

  private void sendSagaAbortedEvent(String methodName, Throwable throwable) {
    // TODO need to check the parentID setting
    sender.send(new SagaAbortedEvent(context.globalTxId(), context.localTxId(), null, methodName, throwable));
  }

}
