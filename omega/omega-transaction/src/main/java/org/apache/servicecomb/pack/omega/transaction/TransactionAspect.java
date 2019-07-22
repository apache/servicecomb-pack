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
import org.apache.servicecomb.pack.omega.context.TransactionContext;
import org.apache.servicecomb.pack.omega.context.TransactionContextProperties;
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
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

  private final CompensableInterceptor interceptor;

  public TransactionAspect(SagaMessageSender sender, OmegaContext context) {
    this.context = context;
    this.context.verify();
    this.interceptor = new CompensableInterceptor(context, sender);
  }

  @Around("execution(@org.apache.servicecomb.pack.omega.transaction.annotations.Compensable * *(..)) && @annotation(compensable)")
  Object advise(ProceedingJoinPoint joinPoint, Compensable compensable) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    // just check if we need to setup the transaction context information first
    TransactionContext transactionContext = getTransactionContextFromArgs(joinPoint.getArgs());
    if (transactionContext != null) {
      if (context.globalTxId() != null) {
        LOG.warn("The context {}'s globalTxId is not empty. Update it for globalTxId:{} and localTxId:{}", context,
            transactionContext.globalTxId(), transactionContext.localTxId());
      } else {
        LOG.debug("Updated context {} for globalTxId:{} and localTxId:{}", context,
            transactionContext.globalTxId(), transactionContext.localTxId());
      }
      context.setGlobalTxId(transactionContext.globalTxId());
      context.setLocalTxId(transactionContext.localTxId());
    }

    String localTxId = context.localTxId();
    context.newLocalTxId();
    LOG.debug("Updated context {} for compensable method {} ", context, method.toString());

    int retries = compensable.retries();
    RecoveryPolicy recoveryPolicy = RecoveryPolicyFactory.getRecoveryPolicy(retries);
    try {
      return recoveryPolicy.apply(joinPoint, compensable, interceptor, context, localTxId, retries);
    } finally {
      context.setLocalTxId(localTxId);
      LOG.debug("Restored context back to {}", context);
    }
  }

  TransactionContext getTransactionContextFromArgs(Object[] args) {
    if (args != null) {
      for (Object arg : args) {
        // check the TransactionContext first
        if (arg instanceof TransactionContext) {
          return (TransactionContext) arg;
        }
        if (arg instanceof TransactionContextProperties) {
          TransactionContextProperties transactionContextProperties = (TransactionContextProperties) arg;
          return new TransactionContext(transactionContextProperties.getGlobalTxId(), transactionContextProperties.getLocalTxId());
        }
      }
    }
    return null;
  }
}
