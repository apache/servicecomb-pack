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

import static io.servicecomb.saga.core.Compensation.SAGA_START_COMPENSATION;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_END_REQUEST;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;
import static io.servicecomb.saga.core.SagaEventMatcher.eventWith;
import static io.servicecomb.saga.core.SagaTask.SAGA_END_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_REQUEST_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_START_TASK;
import static io.servicecomb.saga.core.Transaction.SAGA_END_TRANSACTION;
import static io.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.seanyinx.github.unit.scaffolding.Randomness;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

@SuppressWarnings("unchecked")
public class SagaIntegrationTest {
  private static final String sagaId = Randomness.uniquify("sagaId");

  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventStore eventStore = new EmbeddedEventStore();

  private final Transaction transaction1 = mock(Transaction.class, "transaction1");
  private final Transaction transaction2 = mock(Transaction.class, "transaction2");
  private final Transaction transaction3 = mock(Transaction.class, "transaction3");

  private final Compensation compensation1 = mock(Compensation.class, "compensation1");
  private final Compensation compensation2 = mock(Compensation.class, "compensation2");
  private final Compensation compensation3 = mock(Compensation.class, "compensation3");

  private final String requestJson = "{}";
  private final SagaRequest request1 = sagaTask("request1", "service1", transaction1, compensation1);
  private final SagaRequest request2 = sagaTask("request2", "service2", transaction2, compensation2);
  private final SagaRequest request3 = sagaTask("request3", "service3", transaction3, compensation3);

  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private final RuntimeException exception = new RuntimeException("oops");

  private final Node<SagaRequest> node1 = new Node<>(1, request1);
  private final Node<SagaRequest> node2 = new Node<>(2, request2);
  private final Node<SagaRequest> node3 = new Node<>(3, request3);
  private final Node<SagaRequest> root = new Node<>(0, SAGA_START_REQUEST);
  private final Node<SagaRequest> leaf = new Node<>(4, SAGA_END_REQUEST);
  private final SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph = new SingleLeafDirectedAcyclicGraph<>(root, leaf);
  private final SuccessfulSagaResponse response = new SuccessfulSagaResponse(200, "blah");

  private Saga saga;
  private final Map<String, SagaTask> tasks = new HashMap<>();

  // root - node1 - node2 - leaf
  @Before
  public void setUp() throws Exception {
    root.addChild(node1);
    node1.addChild(node2);
    node2.addChild(leaf);

    SagaStartTask sagaStartTask = new SagaStartTask(sagaId, requestJson, eventStore);
    SagaEndTask sagaEndTask = new SagaEndTask(sagaId, eventStore);
    RequestProcessTask processTask = new RequestProcessTask(sagaId, eventStore);

    tasks.put(SAGA_START_TASK, sagaStartTask);
    tasks.put(SAGA_REQUEST_TASK, processTask);
    tasks.put(SAGA_END_TASK, sagaEndTask);

    saga = new Saga(eventStore, tasks, sagaTaskGraph);
  }

  @Test
  public void transactionsAreRunSuccessfully() {
    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName());
    verify(transaction2).send(request2.serviceName());

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
  }

  // root - node1 - node2 - leaf
  //             \_ node3 _/
  @Test
  public void compensateCommittedTransactionsOnFailure() {
    addExtraChildToNode1();

    // barrier to make sure the two transactions starts at the same time
    CyclicBarrier barrier = new CyclicBarrier(2);
    when(transaction2.send(request2.serviceName()))
        .thenAnswer(
            withAnswer(() -> {
              barrier.await();
              Thread.sleep(100);
              throw exception;
            }));

    when(transaction3.send(request3.serviceName()))
        .thenAnswer(
            withAnswer(() -> {
              barrier.await();
              return null;
            }));

    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        anyOf(eventWith(sagaId, transaction2, TransactionStartedEvent.class), eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        anyOf(eventWith(sagaId, transaction2, TransactionStartedEvent.class), eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionAbortedEvent.class),
        eventWith(sagaId, compensation3, CompensationStartedEvent.class),
        eventWith(sagaId, compensation3, CompensationEndedEvent.class),
        eventWith(sagaId, compensation1, CompensationStartedEvent.class),
        eventWith(sagaId, compensation1, CompensationEndedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).send(request1.serviceName());
    verify(transaction2).send(request2.serviceName());
    verify(transaction3).send(request3.serviceName());

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3).send(request3.serviceName());
  }

  // root - node1 - node2 - leaf
  //             \_ node4 _/
  @Test
  public void redoHangingTransactionsOnFailure() throws InterruptedException {
    addExtraChildToNode1();

    // barrier to make sure the two transactions starts at the same time
    CyclicBarrier barrier = new CyclicBarrier(2);
    when(transaction3.send(request3.serviceName()))
        .thenAnswer(withAnswer(() -> {
      barrier.await();
      throw exception;
    }));

    CountDownLatch latch = new CountDownLatch(1);

    when(transaction2.send(request2.serviceName()))
        .thenAnswer(withAnswer(() -> {
      barrier.await();
      latch.await();
      return null;
    })).thenReturn(response);

    saga.run();

    // the ordering of events may not be consistence due to concurrent processing of requests
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        anyOf(
            eventWith(sagaId, transaction2, TransactionStartedEvent.class),
            eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        anyOf(
            eventWith(sagaId, transaction3, TransactionStartedEvent.class),
            eventWith(sagaId, transaction2, TransactionStartedEvent.class)),
        eventWith(sagaId, transaction3, TransactionAbortedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, compensation2, CompensationStartedEvent.class),
        eventWith(sagaId, compensation2, CompensationEndedEvent.class),
        eventWith(sagaId, compensation1, CompensationStartedEvent.class),
        eventWith(sagaId, compensation1, CompensationEndedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).send(request1.serviceName());
    verify(transaction2, times(2)).send(request2.serviceName());
    verify(transaction3).send(request3.serviceName());

    verify(compensation1).send(request1.serviceName());
    verify(compensation2).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());

    latch.countDown();
  }

  @Test
  public void retriesFailedTransactionTillSuccess() {
    Saga saga = new Saga(eventStore, new ForwardRecovery(), tasks, sagaTaskGraph);

    when(transaction2.send(request2.serviceName()))
        .thenThrow(exception).thenThrow(exception).thenReturn(response);

    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName());
    verify(transaction2, times(3)).send(request2.serviceName());

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
  }

  @Test
  public void restoresSagaToTransactionStateByPlayingAllEvents() {
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(request1.serviceName());
    verify(transaction2, never()).send(request2.serviceName());
    verify(transaction3).send(request3.serviceName());

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresPartialTransactionByPlayingAllEvents() {
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(request1.serviceName());
    verify(transaction2, never()).send(request2.serviceName());
    verify(transaction3).send(request3.serviceName());

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresToCompensationFromAbortedTransactionByPlayingAllEvents() {
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3)),
        envelope(new TransactionAbortedEvent(sagaId, request3, exception))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionAbortedEvent.class),
        eventWith(sagaId, compensation2, CompensationStartedEvent.class),
        eventWith(sagaId, compensation2, CompensationEndedEvent.class),
        eventWith(sagaId, compensation1, CompensationStartedEvent.class),
        eventWith(sagaId, compensation1, CompensationEndedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(request1.serviceName());
    verify(transaction2, never()).send(request2.serviceName());
    verify(transaction3, never()).send(request3.serviceName());

    verify(compensation1).send(request1.serviceName());
    verify(compensation2).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresSagaToCompensationStateByPlayingAllEvents() {
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3)),
        envelope(new TransactionEndedEvent(sagaId, request3)),
        envelope(new CompensationStartedEvent(sagaId, request2)),
        envelope(new CompensationEndedEvent(sagaId, request2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, compensation2, CompensationStartedEvent.class),
        eventWith(sagaId, compensation2, CompensationEndedEvent.class),
        eventWith(sagaId, compensation3, CompensationStartedEvent.class),
        eventWith(sagaId, compensation3, CompensationEndedEvent.class),
        eventWith(sagaId, compensation1, CompensationStartedEvent.class),
        eventWith(sagaId, compensation1, CompensationEndedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(request1.serviceName());
    verify(transaction2, never()).send(request2.serviceName());
    verify(transaction3, never()).send(request3.serviceName());

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3).send(request3.serviceName());
  }

  @Test
  public void restoresPartialCompensationByPlayingAllEvents() {
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3)),
        envelope(new TransactionEndedEvent(sagaId, request3)),
        envelope(new CompensationStartedEvent(sagaId, request2)),
        envelope(new CompensationEndedEvent(sagaId, request2)),
        envelope(new CompensationStartedEvent(sagaId, request3))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, compensation2, CompensationStartedEvent.class),
        eventWith(sagaId, compensation2, CompensationEndedEvent.class),
        eventWith(sagaId, compensation3, CompensationStartedEvent.class),
        eventWith(sagaId, compensation3, CompensationStartedEvent.class),
        eventWith(sagaId, compensation3, CompensationEndedEvent.class),
        eventWith(sagaId, compensation1, CompensationStartedEvent.class),
        eventWith(sagaId, compensation1, CompensationEndedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(request1.serviceName());
    verify(transaction2, never()).send(request2.serviceName());
    verify(transaction3, never()).send(request3.serviceName());

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3).send(request3.serviceName());
  }

  @Test
  public void restoresSagaToEndStateByPlayingAllEvents() {
    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(request1.serviceName());
    verify(transaction2, never()).send(request2.serviceName());

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
  }

  private Answer<Void> withAnswer(Callable<Void> callable) {
    return invocationOnMock -> callable.call();
  }

  private EventEnvelope envelope(SagaEvent event) {
    return new EventEnvelope(idGenerator.nextId(), event);
  }

  private void addExtraChildToNode1() {
    node1.addChild(node3);
    node3.addChild(leaf);
  }

  private SagaRequest sagaTask(String requestId,
      String serviceName,
      Transaction transaction,
      Compensation compensation) {

    return new SagaRequestImpl(requestId, serviceName, "rest", transaction, compensation);
  }
}
