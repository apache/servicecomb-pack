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

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TransactionStateTest {

  private final SagaTask sagaTask1 = Mockito.mock(SagaTask.class);
  private final SagaTask sagaTask2 = Mockito.mock(SagaTask.class);

  private final Deque<SagaTask> executedTasks = new LinkedList<>();
  private final Queue<SagaTask> pendingTasks = new LinkedList<>();

  @Before
  public void setUp() throws Exception {
    pendingTasks.offer(sagaTask1);
    pendingTasks.offer(sagaTask2);
  }

  @Test
  public void transferHeadFromPendingTasksToExecutedOnSuccess() {
    TransactionState.INSTANCE.invoke(executedTasks, pendingTasks);

    assertThat(executedTasks, contains(sagaTask1));
    assertThat(pendingTasks, contains(sagaTask2));
  }

  @Test
  public void doNotConsumeHeadFromPendingTasksOnFailure() {
    doThrow(new RuntimeException("oops")).when(sagaTask1).commit();

    try {
      TransactionState.INSTANCE.invoke(executedTasks, pendingTasks);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException ignored) {
    }

    assertThat(executedTasks, contains(sagaTask1));
    assertThat(pendingTasks, contains(sagaTask1, sagaTask2));
  }
}