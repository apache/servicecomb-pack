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

import static io.servicecomb.saga.core.Compensation.SAGA_END_COMPENSATION;
import static io.servicecomb.saga.core.Compensation.SAGA_START_COMPENSATION;
import static io.servicecomb.saga.core.SagaEventMatcher.eventWith;
import static io.servicecomb.saga.core.Transaction.SAGA_END_TRANSACTION;
import static io.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;
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

  private final EventStore eventStore = new EmbeddedEventStore();

  private final RequestProcessTask processCommand = new RequestProcessTask(eventStore);

  private final TaskAwareSagaRequest sagaStartRequest = sagaStartRequest();
  private final SagaRequest request1 = new TaskAwareSagaRequest("request1", transaction1, compensation1, processCommand);
  private final SagaRequest request2 = new TaskAwareSagaRequest("request2", transaction2, compensation2, processCommand);
  private final SagaRequest request3 = new TaskAwareSagaRequest("request3", transaction3, compensation3, processCommand);

  private final Node<SagaRequest> node1 = new Node<>(1, request1);
  private final Node<SagaRequest> node2 = new Node<>(2, request2);
  private final Node<SagaRequest> node3 = new Node<>(3, request3);
  private final Node<SagaRequest> root = new Node<>(0, sagaStartRequest);
  private final Node<SagaRequest> leaf = new Node<>(4, sagaEndRequest());

  private final SagaCoordinator coordinator = new SagaCoordinator(eventStore,
      new SingleLeafDirectedAcyclicGraph<>(root, leaf));

  @Before
  public void setUp() throws Exception {
    root.addChild(node1);
    node1.addChild(node2);
    node2.addChild(node3);
    node3.addChild(leaf);
  }

  @Test
  public void recoverSagaWithEventsFromEventStore() {
    eventStore.offer(new SagaStartedEvent(sagaStartRequest));
    eventStore.offer(new TransactionStartedEvent(request1));
    eventStore.offer(new TransactionEndedEvent(request1));
    eventStore.offer(new TransactionStartedEvent(request2));

    coordinator.run();

    assertThat(eventStore, contains(
        eventWith(1L, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionStartedEvent.class),
        eventWith(6L, transaction2, TransactionEndedEvent.class),
        eventWith(7L, transaction3, TransactionStartedEvent.class),
        eventWith(8L, transaction3, TransactionEndedEvent.class),
        eventWith(9L, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));
  }

  private TaskAwareSagaRequest sagaStartRequest() {
    return new TaskAwareSagaRequest("saga-start", SAGA_START_TRANSACTION, SAGA_START_COMPENSATION, new SagaStartTask(eventStore));
  }

  private TaskAwareSagaRequest sagaEndRequest() {
    return new TaskAwareSagaRequest("saga-end", SAGA_END_TRANSACTION, SAGA_END_COMPENSATION, new SagaEndTask(eventStore));
  }
}