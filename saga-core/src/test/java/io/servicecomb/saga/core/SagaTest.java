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

package io.servicecomb.saga.core;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class SagaTest {

  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventQueue eventQueue = new EmbeddedEventQueue();

  private final Transaction transaction1 = mock(Transaction.class);
  private final Transaction transaction2 = mock(Transaction.class);
  private final Transaction transaction3 = mock(Transaction.class);

  private final Sage sage = new Sage(idGenerator, eventQueue, transaction1, transaction2, transaction3);

  @Test
  public void transactionsAreRunSuccessfully() {
    sage.run();

    assertThat(idsOf(eventQueue), contains(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

    verify(transaction1).execute();
    verify(transaction2).execute();
    verify(transaction3).execute();
  }

  private List<Long> idsOf(EventQueue eventQueue) {
    List<Long> ids = new ArrayList<>(eventQueue.size());
    for (SagaEvent event : eventQueue) {
      ids.add(event.id());
    }

    return ids;
  }
}
