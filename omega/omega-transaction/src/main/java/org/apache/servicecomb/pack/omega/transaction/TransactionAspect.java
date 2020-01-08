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
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Aspect
@Order(value = 200)
public class TransactionAspect extends TransactionContextHelper {

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
    TransactionContext transactionContext = extractTransactionContext(joinPoint.getArgs());
    if (transactionContext != null) {
      populateOmegaContext(context, transactionContext);
    }
    // SCB-1011 Need to check if the globalTxId transaction is null to avoid the message sending failure
    if (context.globalTxId() == null) {
      throw new OmegaException("Cannot find the globalTxId from OmegaContext. Please using @SagaStart to start a global transaction.");
    }
    String localTxId = context.localTxId();
    context.newLocalTxId();
    LOG.debug("Updated context {} for compensable method {} ", context, method.toString());

    int forwardRetries = compensable.forwardRetries();
    RecoveryPolicy recoveryPolicy = RecoveryPolicyFactory.getRecoveryPolicy(forwardRetries);
    try {
      return recoveryPolicy.apply(joinPoint, compensable, interceptor, context, localTxId, forwardRetries);
    } finally {
      context.setLocalTxId(localTxId);
      LOG.debug("Restored context back to {}", context);
    }
  }

  @Override
  protected Logger getLogger() {
    return LOG;
  }

}
