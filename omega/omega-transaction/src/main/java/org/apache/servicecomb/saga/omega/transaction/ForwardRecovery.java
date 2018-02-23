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
 * ForwardRecovery is used to execute business logic with the given retries times.
 * If retries is above 0, it will retry the given times at most.
 * If retries == -1, it will retry forever until interrupted.
 */
public class ForwardRecovery extends DefaultRecovery {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // TODO: 2018/03/10 we do not support retry with timeout yet
  @Override
  public Object apply(ProceedingJoinPoint joinPoint, Compensable compensable, CompensableInterceptor interceptor,
      OmegaContext context, String parentTxId, int retries) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    int remains = retries;
    try {
      while (true) {
        try {
          return super.apply(joinPoint, compensable, interceptor, context, parentTxId, remains);
        } catch (Throwable throwable) {
          if (throwable instanceof InvalidTransactionException) {
            throw throwable;
          }

          remains = remains == -1 ? -1 : remains - 1;
          if (remains == 0) {
            LOG.error(
                "Retried sub tx failed maximum times, global tx id: {}, local tx id: {}, method: {}, retried times: {}",
                context.globalTxId(), context.localTxId(), method.toString(), retries);
            throw throwable;
          }

          LOG.warn("Retrying sub tx failed, global tx id: {}, local tx id: {}, method: {}, remains: {}",
              context.globalTxId(), context.localTxId(), method.toString(), remains);
          Thread.sleep(compensable.retryDelayInMilliseconds());
        }
      }
    } catch (InterruptedException e) {
      String errorMessage = "Failed to handle tx because it is interrupted, global tx id: " + context.globalTxId()
          + ", local tx id: " + context.localTxId() + ", method: " + method.toString();
      LOG.error(errorMessage);
      interceptor.onError(parentTxId, compensationMethodSignature(joinPoint, compensable, method), e);
      throw new OmegaException(errorMessage);
    }
  }
}
