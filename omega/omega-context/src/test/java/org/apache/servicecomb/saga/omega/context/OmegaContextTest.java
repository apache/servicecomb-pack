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

package org.apache.servicecomb.saga.omega.context;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;

public class OmegaContextTest {

  private final OmegaContext omegaContext = new OmegaContext(() -> "ignored");

  @Test
  public void eachThreadGetsDifferentGlobalTxId() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(() -> {
      String txId = UUID.randomUUID().toString();
      omegaContext.setGlobalTxId(txId);
      barrier.await();

      assertThat(omegaContext.globalTxId(), is(txId));
    });

    CompletableFuture<Void> future1 = CompletableFuture.runAsync(runnable);
    CompletableFuture<Void> future2 = CompletableFuture.runAsync(runnable);

    CompletableFuture.allOf(future1, future2).join();
  }

  @Test
  public void eachThreadGetsDifferentLocalTxId() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(() -> {
      String spanId = UUID.randomUUID().toString();
      omegaContext.setLocalTxId(spanId);
      barrier.await();

      assertThat(omegaContext.localTxId(), is(spanId));
    });

    CompletableFuture<Void> future1 = CompletableFuture.runAsync(runnable);
    CompletableFuture<Void> future2 = CompletableFuture.runAsync(runnable);

    CompletableFuture.allOf(future1, future2).join();
  }

  private Runnable exceptionalRunnable(ExceptionalRunnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (Exception e) {
        fail(e.getMessage());
      }
    };
  }

  interface ExceptionalRunnable {
    void run() throws Exception;
  }
}
