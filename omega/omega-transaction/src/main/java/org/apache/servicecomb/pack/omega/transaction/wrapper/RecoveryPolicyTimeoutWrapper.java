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

import java.lang.invoke.MethodHandles;
import java.nio.channels.ClosedByInterruptException;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.transaction.AbstractRecoveryPolicy;
import org.apache.servicecomb.pack.omega.transaction.CompensableInterceptor;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.TransactionTimeoutException;
import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RecoveryPolicy Wrapper
 * 1.Use this wrapper to send a request if the @Compensable timeout>0
 * 2.Terminate thread execution if execution time is greater than the timeout of @Compensable
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

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  //private static RecoveryPolicyTimeoutWrapper instance = new RecoveryPolicyTimeoutWrapper();
  private AbstractRecoveryPolicy recoveryPolicy;
  //private final transient Set<TimeoutProb> timeoutProbs = new ConcurrentSkipListSet<TimeoutProb>();

//  public static RecoveryPolicyTimeoutWrapper getInstance() {
//    return instance;
//  }

  public RecoveryPolicyTimeoutWrapper(AbstractRecoveryPolicy recoveryPolicy) {
    this.recoveryPolicy = recoveryPolicy;
//    this.interrupter.scheduleWithFixedDelay(
//        new Runnable() {
//          @Override
//          public void run() {
//            try {
//              RecoveryPolicyTimeoutWrapper.this.interrupt();
//            } catch (Exception e) {
//              LOG.error("The overtime thread interrupt fail",e);
//            }
//          }
//        },
//        0, delay, TimeUnit.MICROSECONDS
//    );
  }

  /**
   * Configuration timeout probe thread
   */
//  private final transient ScheduledExecutorService interrupter =
//      Executors.newSingleThreadScheduledExecutor(
//          new TimeoutProbeThreadFactory()
//      );

  /**
   * Loop detection of all thread timeout probes, remove probe if the thread has terminated
   */
//  private void interrupt() {
//    synchronized (this.interrupter) {
//      for (TimeoutProb timeoutProb : this.timeoutProbs) {
//        if (timeoutProb.interruptFailureException == null) {
//          if (timeoutProb.expired()) {
//            if (timeoutProb.interrupted()) {
//              this.timeoutProbs.remove(timeoutProb);
//            }
//          }
//        }
//      }
//    }
//  }

//  public RecoveryPolicyTimeoutWrapper wrapper(AbstractRecoveryPolicy recoveryPolicy) {
//    this.recoveryPolicy = recoveryPolicy;
//    return this;
//  }

  public Object applyTo(ProceedingJoinPoint joinPoint, Compensable compensable,
      CompensableInterceptor interceptor, OmegaContext context, String parentTxId, int retries)
      throws Throwable {
    final TimeoutProb timeoutProb = TimeoutProbManager.getInstance().addTimeoutProb(compensable.timeout());
    Object output;
    try {
      output = this.recoveryPolicy
          .applyTo(joinPoint, compensable, interceptor, context, parentTxId, retries);
      if (timeoutProb.getInterruptFailureException() != null) {
        throw new OmegaException(timeoutProb.getInterruptFailureException());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
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

  /**
   * Define timeout probe
   */
//  private static final class TimeoutProb implements
//      Comparable<TimeoutProb> {
//
//    private final transient Thread thread = Thread.currentThread();
//    private final transient long startTime = System.currentTimeMillis();
//    private final transient long expireTime;
//    private Exception interruptFailureException = null;
//    private final transient ProceedingJoinPoint joinPoint;
//
//    public TimeoutProb(final ProceedingJoinPoint pnt, Compensable compensable) {
//      this.joinPoint = pnt;
//      this.expireTime = this.startTime + TimeUnit.SECONDS.toMillis(compensable.timeout());
//    }
//
//    @Override
//    public int compareTo(final TimeoutProb obj) {
//      int compare;
//      if (this.expireTime > obj.expireTime) {
//        compare = 1;
//      } else if (this.expireTime < obj.expireTime) {
//        compare = -1;
//      } else {
//        compare = 0;
//      }
//      return compare;
//    }
//
//    public Exception getInterruptFailureException() {
//      return interruptFailureException;
//    }
//
//    /**
//     *
//     * @return Returns TRUE if expired
//     */
//    public boolean expired() {
//      return this.expireTime < System.currentTimeMillis();
//    }
//
//    /**
//     * Interrupt thread
//     *
//     * @return Returns TRUE if the thread has been interrupted
//     */
//    public boolean interrupted() {
//      boolean interrupted;
//      if (this.thread.isAlive()) {
//        // 如果当前线程是活动状态，则发送线程中断信号
//        try {
//          this.thread.interrupt();
//        } catch (Exception e) {
//          this.interruptFailureException = e;
//          LOG.info("Failed to interrupt the thread " + this.thread.getName(), e);
//          throw e;
//        }
//        final Method method = MethodSignature.class.cast(this.joinPoint.getSignature()).getMethod();
//        LOG.warn("{}: interrupted on {}ms timeout (over {}ms)",
//            new Object[]{method, System.currentTimeMillis() - this.startTime,
//                this.expireTime - this.startTime}
//        );
//        interrupted = false;
//      } else {
//        interrupted = true;
//      }
//      return interrupted;
//    }
//  }
//
//  public class TimeoutProbeThreadFactory implements ThreadFactory {
//
//    public Thread newThread(Runnable runnable) {
//      Thread thread = new Thread(new ThreadGroup("recovery-policy-timeout-wrapper"), runnable,
//          "probe");
//      thread.setPriority(Thread.MAX_PRIORITY);
//      thread.setDaemon(true);
//      return thread;
//    }
//  }
}
