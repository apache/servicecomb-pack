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

package org.apache.servicecomb.pack.omega.transaction.wrapper;

import java.nio.channels.ClosedByInterruptException;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.transaction.AbstractRecoveryPolicy;
import org.apache.servicecomb.pack.omega.transaction.CompensableInterceptor;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.TransactionTimeoutException;
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * RecoveryPolicy Wrapper
 * 1.Use this wrapper to send a request if the @Compensable forwardTimeout greaterThan 0
 * 2.Terminate thread execution if execution time is greater than the forwardTimeout of @Compensable
 *
 * Exception
 * 1.If the interrupt succeeds, a TransactionTimeoutException is thrown and the local transaction is rollback
 * 2.If the interrupt fails, throw an OmegaException
 *
 * Note: Omega end thread coding advice
 * 1.add short sleep to while true loop. Otherwise, the thread may not be able to terminate.
 * 2.Replace the synchronized with ReentrantLock, Otherwise, the thread may not be able to terminate.
 * */

public class RecoveryPolicyTimeoutWrapper {

  private AbstractRecoveryPolicy recoveryPolicy;

  public RecoveryPolicyTimeoutWrapper(AbstractRecoveryPolicy recoveryPolicy) {
    this.recoveryPolicy = recoveryPolicy;
  }

  public Object applyTo(ProceedingJoinPoint joinPoint, Compensable compensable,
      CompensableInterceptor interceptor, OmegaContext context, String parentTxId, int retries)
      throws Throwable {
    final TimeoutProb timeoutProb = TimeoutProbManager.getInstance().addTimeoutProb(compensable.forwardTimeout());
    Object output;
    try {
      output = this.recoveryPolicy
          .applyTo(joinPoint, compensable, interceptor, context, parentTxId, retries);
      if (timeoutProb.getInterruptFailureException() != null) {
        throw new OmegaException(timeoutProb.getInterruptFailureException());
      }
    } catch (InterruptedException e) {
      if (timeoutProb.getInterruptFailureException() != null) {
        throw new OmegaException(timeoutProb.getInterruptFailureException());
      }else{
        throw new TransactionTimeoutException(e.getMessage(),e);
      }
    } catch (IllegalMonitorStateException e) {
      if (timeoutProb.getInterruptFailureException() != null) {
        throw new OmegaException(timeoutProb.getInterruptFailureException());
      }else{
        throw new TransactionTimeoutException(e.getMessage(),e);
      }
    } catch (ClosedByInterruptException e) {
      if (timeoutProb.getInterruptFailureException() != null) {
        throw new OmegaException(timeoutProb.getInterruptFailureException());
      }else{
        throw new TransactionTimeoutException(e.getMessage(),e);
      }
    } catch (Throwable e) {
      throw e;
    } finally {
      TimeoutProbManager.getInstance().removeTimeoutProb(timeoutProb);
    }
    return output;
  }
}
