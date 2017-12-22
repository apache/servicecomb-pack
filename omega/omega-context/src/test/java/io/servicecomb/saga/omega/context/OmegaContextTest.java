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

package io.servicecomb.saga.omega.context;

import static com.seanyinx.github.unit.scaffolding.Randomness.nextId;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;

public class OmegaContextTest {

  private final OmegaContext omegaContext = new OmegaContext();

  @Test
  public void eachThreadGetsDifferentId() throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(() -> {
      long txId = nextId();
      omegaContext.setTxId(txId);
      barrier.await();

      assertThat(omegaContext.txId(), is(txId));
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
