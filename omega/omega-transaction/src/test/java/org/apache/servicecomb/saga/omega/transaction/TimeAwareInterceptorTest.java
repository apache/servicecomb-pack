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

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TimeAwareInterceptorTest {
  private static final int runningCounts = 1000;

  private final String localTxId = uniquify("localTxId");
  private final String signature = uniquify("signature");

  private final AtomicInteger postInterceptInvoked = new AtomicInteger();
  private final AtomicInteger onErrorInvoked = new AtomicInteger();
  private final AtomicInteger onTimeoutInvoked = new AtomicInteger();

  private final EventAwareInterceptor underlying = new EventAwareInterceptor() {
    @Override
    public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, Object... message) {
      return new AlphaResponse(false);
    }

    @Override
    public void postIntercept(String parentTxId, String compensationMethod) {
      postInterceptInvoked.incrementAndGet();
    }

    @Override
    public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
      if (throwable instanceof OmegaTxTimeoutException) {
        onTimeoutInvoked.incrementAndGet();
      } else {
        onErrorInvoked.incrementAndGet();
      }
    }
  };

  private final ExecutorService executorService = Executors.newFixedThreadPool(2);
  private final RuntimeException timeoutException = new OmegaTxTimeoutException("timed out");


  @Test(timeout = 5000)
  public void invokeEitherPostInterceptOrOnTimeoutConcurrently() throws Exception {
    List<Future<?>> futures = new LinkedList<>();

    for (int i = 0; i < runningCounts; i++) {
      TimeAwareInterceptor interceptor = new TimeAwareInterceptor(underlying);
      CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      ExpectedException exception = ExpectedException.none();

      futures.add(executorService.submit(() -> {
        try {
          waitForSignal(cyclicBarrier);
          interceptor.postIntercept(localTxId, signature);
        } catch (Throwable throwable) {
          exception.expect(OmegaTxTimeoutException.class);
        }
      }));

      futures.add(executorService.submit(() -> {
        waitForSignal(cyclicBarrier);
        interceptor.onTimeout(localTxId, signature, timeoutException);
      }));
    }

    waitTillAllDone(futures);

    assertThat(postInterceptInvoked.get() + onTimeoutInvoked.get(), is(runningCounts));
  }

  @Test(timeout = 5000)
  public void invokeEitherOnErrorOrOnTimeoutConcurrently() throws Exception {
    RuntimeException oops = new RuntimeException("oops");
    List<Future<?>> futures = new LinkedList<>();

    for (int i = 0; i < runningCounts; i++) {
      TimeAwareInterceptor interceptor = new TimeAwareInterceptor(underlying);
      CyclicBarrier cyclicBarrier = new CyclicBarrier(2);


      futures.add(executorService.submit(() -> {
        waitForSignal(cyclicBarrier);
        interceptor.onError(localTxId, signature, oops);
      }));

      futures.add(executorService.submit(() -> {
        waitForSignal(cyclicBarrier);
        interceptor.onTimeout(localTxId, signature, timeoutException);
      }));
    }

    waitTillAllDone(futures);

    assertThat(onErrorInvoked.get() + onTimeoutInvoked.get(), is(runningCounts));
  }

  private void waitForSignal(CyclicBarrier cyclicBarrier) {
    try {
      cyclicBarrier.await();
    } catch (InterruptedException | BrokenBarrierException e) {
      fail(e.getMessage());
    }
  }

  private void waitTillAllDone(List<Future<?>> futures)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    for (Future<?> future : futures) {
      future.get();
    }
  }
}
