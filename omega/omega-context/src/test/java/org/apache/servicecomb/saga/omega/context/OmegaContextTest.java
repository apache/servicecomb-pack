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

import java.io.Serializable;
import java.util.UUID;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;


public class OmegaContextTest {

  private final OmegaContext omegaContext = new OmegaContext(
      new IdGenerator() {
        @Override
        public Serializable nextId() {
          return "ignored";
        }
      });

  ExecutorService executor = Executors.newFixedThreadPool(2);



  @Test
  public void eachThreadGetsDifferentGlobalTxId() throws Exception {
    final CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(new ExceptionalRunnable() {

      @Override
      public void run() throws Exception {
        String txId = UUID.randomUUID().toString();
        omegaContext.setGlobalTxId(txId);
        barrier.await();

        assertThat(omegaContext.globalTxId(), is(txId));
      }
    });

    Future f1 = executor.submit(runnable);                                      ;
    Future f2 = executor.submit(runnable);
    f1.get();
    f2.get();

  }

  @Test
  public void eachThreadGetsDifferentLocalTxId() throws Exception {
    final CyclicBarrier barrier = new CyclicBarrier(2);

    Runnable runnable = exceptionalRunnable(new ExceptionalRunnable() {

      @Override
      public void run() throws Exception {
        String spanId = UUID.randomUUID().toString();
        omegaContext.setLocalTxId(spanId);
        barrier.await();

        assertThat(omegaContext.localTxId(), is(spanId));
      }
    });

    Future f1 = executor.submit(runnable);                                      ;
    Future f2 = executor.submit(runnable);
    f1.get();
    f2.get();
  }

  private Runnable exceptionalRunnable(final ExceptionalRunnable runnable) {
    return new Runnable() {

      @Override
      public void run() {
        try {
          runnable.run();
        } catch (Exception e) {
          fail(e.getMessage());
        }
      }
    };
  }


  interface ExceptionalRunnable {
    void run() throws Exception;
  }
}
