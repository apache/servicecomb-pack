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

package org.apache.servicecomb.saga.core;


import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.apache.servicecomb.saga.core.SagaTaskFactory.RetrySagaLog;

public class RetrySagaLogTest {

  private final PersistentStore persistentStore = mock(PersistentStore.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaEvent dummyEvent = new DummyEvent(sagaRequest);
  private final RetrySagaLog retrySagaLog = new RetrySagaLog(persistentStore, 100);

  private volatile boolean interrupted = false;

  @Test
  public void retryUntilSuccessWhenEventIsNotPersisted() throws InterruptedException {
    doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doThrow(RuntimeException.class).
        doNothing().
        when(persistentStore).offer(dummyEvent);

    retrySagaLog.offer(dummyEvent);

    verify(persistentStore, times(6)).offer(dummyEvent);
  }

  @Test
  public void exitOnInterruption() throws InterruptedException {
    ExecutorService executor = Executors.newSingleThreadExecutor();

    Future<?> future = executor.submit(() -> {
      doThrow(RuntimeException.class).when(persistentStore).offer(dummyEvent);

      retrySagaLog.offer(dummyEvent);
      interrupted = true;
    });

    Thread.sleep(500);

    assertThat(future.cancel(true), is(true));

    await().atMost(2, TimeUnit.SECONDS).until(() -> interrupted);
    executor.shutdown();
  }
}
