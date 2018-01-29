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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class OnceAwareInterceptorTest {
  private static final int runningCounts = 1000;

  private final String localTxId = uniquify("localTxId");
  private final String signature = uniquify("signature");

  private final AtomicInteger postInterceptInvoked = new AtomicInteger();
  private final AtomicInteger onErrorInvoked = new AtomicInteger();

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
      onErrorInvoked.incrementAndGet();
    }
  };

  private final ExecutorService executorService = Executors.newFixedThreadPool(2);

  @Test
  public void invokePostIntercept() throws Exception {
    List<Future<?>> futures = new LinkedList<>();

    for (int i = 0; i < runningCounts; i++) {
      OnceAwareInterceptor interceptor = new OnceAwareInterceptor(underlying);

      futures.add(executorService.submit(() -> interceptor.postIntercept(localTxId, signature)));
    }

    waitTillAllDone(futures);

    assertThat(postInterceptInvoked.get(), is(runningCounts));
  }

  @Test
  public void invokeOnErrorConcurrently() throws Exception {
    RuntimeException oops = new RuntimeException("oops");
    List<Future<?>> futures = new LinkedList<>();

    for (int i = 0; i < runningCounts; i++) {
      OnceAwareInterceptor interceptor = new OnceAwareInterceptor(underlying);

      futures.add(executorService.submit(() -> interceptor.onError(localTxId, signature, oops)));
    }

    waitTillAllDone(futures);

    assertThat(onErrorInvoked.get(), is(runningCounts));
  }

  private void waitTillAllDone(List<Future<?>> futures)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    for (Future<?> future : futures) {
      future.get();
    }
  }
}
