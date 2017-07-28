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

import static io.servicecomb.saga.core.Compensation.NO_OP_COMPENSATION;
import static io.servicecomb.saga.core.Transaction.NO_OP_TRANSACTION;
import static io.servicecomb.saga.core.SagaEventMatcher.eventWith;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;
import org.junit.Before;
import org.junit.Test;

public class SagaCoordinatorTest {

  private final Transaction transaction1 = mock(Transaction.class, "transaction1");
  private final Transaction transaction2 = mock(Transaction.class, "transaction2");
  private final Transaction transaction3 = mock(Transaction.class, "transaction3");

  private final Compensation compensation1 = mock(Compensation.class, "compensation1");
  private final Compensation compensation2 = mock(Compensation.class, "compensation2");
  private final Compensation compensation3 = mock(Compensation.class, "compensation3");

  private final SagaRequest request1 = new SagaRequest(transaction1, compensation1);
  private final SagaRequest request2 = new SagaRequest(transaction2, compensation2);
  private final SagaRequest request3 = new SagaRequest(transaction3, compensation3);

  private final EventStore eventStore = new EmbeddedEventStore();
  private final IdGenerator<Long> idGenerator = new LongIdGenerator();

  private final SagaTask sagaStartTask = new SagaStartTask(0L, eventStore, idGenerator);
  private final SagaTask task1 = new RequestProcessTask(1L, request1, eventStore, idGenerator);
  private final SagaTask task2 = new RequestProcessTask(2L, request2, eventStore, idGenerator);
  private final SagaTask task3 = new RequestProcessTask(3L, request3, eventStore, idGenerator);
  private final SagaTask sagaEndTask = new SagaEndTask(5L, eventStore, idGenerator);

  private final Node<SagaTask> node1 = new Node<>(task1.id(), task1);
  private final Node<SagaTask> node2 = new Node<>(task2.id(), task2);
  private final Node<SagaTask> node3 = new Node<>(task3.id(), task3);
  private final Node<SagaTask> root = new Node<>(sagaStartTask.id(), sagaStartTask);
  private final Node<SagaTask> leaf = new Node<>(sagaEndTask.id(), sagaEndTask);

  private final SagaCoordinator coordinator = new SagaCoordinator(eventStore,
      idGenerator, new SingleLeafDirectedAcyclicGraph<>(root, leaf));

  @Before
  public void setUp() throws Exception {
    root.addChild(node1);
    node1.addChild(node2);
    node2.addChild(node3);
    node3.addChild(leaf);
  }

  @Test
  public void recoverSagaWithEventsFromEventStore() {
    eventStore.offer(new SagaStartedEvent(1L, sagaStartTask));
    eventStore.offer(new TransactionStartedEvent(2L, task1));
    eventStore.offer(new TransactionEndedEvent(3L, task1));
    eventStore.offer(new TransactionStartedEvent(4L, task2));

    coordinator.run();

    assertThat(eventStore, contains(
        eventWith(1L, NO_OP_TRANSACTION, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionEndedEvent.class),
        eventWith(6L, transaction3, TransactionStartedEvent.class),
        eventWith(7L, transaction3, TransactionEndedEvent.class),
        eventWith(8L, NO_OP_COMPENSATION, SagaEndedEvent.class)
    ));
  }
}