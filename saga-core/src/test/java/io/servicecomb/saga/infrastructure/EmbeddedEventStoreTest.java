/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.infrastructure;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import io.servicecomb.saga.core.DummyEvent;
import io.servicecomb.saga.core.DummyTask;
import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.EventStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;

public class EmbeddedEventStoreTest {

  private final ExecutorService executorService = Executors.newFixedThreadPool(10);
  private final EventStore eventStore = new EmbeddedEventStore();

  @Test
  public void eventsAreAddedInOrder() {
    CountDownLatch latch = new CountDownLatch(1);
    for (int i = 0; i < 100; i++) {
      executorService.execute(() -> {
        try {
          latch.await();
          eventStore.offer(new DummyEvent(new DummyTask()));
        } catch (InterruptedException ignored) {
        }
      });
    }

    latch.countDown();

    await().atMost(5, SECONDS).until(
        () -> {
          long i = 1;
          for (EventEnvelope event : eventStore) {
            assertThat(event.id, is(i));
            i++;
          }
          return true;
        }
    );
  }
}