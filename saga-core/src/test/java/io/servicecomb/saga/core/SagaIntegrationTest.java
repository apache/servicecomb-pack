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
import static io.servicecomb.saga.core.SagaEventMatcher.eventWith;
import static io.servicecomb.saga.core.Transaction.NO_OP_TRANSACTION;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class SagaIntegrationTest {

  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventStore eventStore = new EmbeddedEventStore();

  private final Transaction transaction1 = mock(Transaction.class, "transaction1");
  private final Transaction transaction2 = mock(Transaction.class, "transaction2");
  private final Transaction transaction3 = mock(Transaction.class, "transaction3");
  private final Transaction transaction4 = mock(Transaction.class, "transaction4");

  private final Transaction[] transactions = {transaction1, transaction2, transaction3};

  private final Compensation compensation1 = mock(Compensation.class, "compensation1");
  private final Compensation compensation2 = mock(Compensation.class, "compensation2");
  private final Compensation compensation3 = mock(Compensation.class, "compensation3");
  private final Compensation compensation4 = mock(Compensation.class, "compensation4");

  private final Compensation[] compensations = {compensation1, compensation2, compensation3};

  private final SagaRequest request1 = new SagaRequest(transaction1, compensation1);
  private final SagaRequest request2 = new SagaRequest(transaction2, compensation2);
  private final SagaRequest request3 = new SagaRequest(transaction3, compensation3);
  private final SagaRequest request4 = new SagaRequest(transaction4, compensation4);

  private final SagaTask sagaStartTask = new SagaStartTask(0L, eventStore, idGenerator);
  private final SagaTask task1 = new RequestProcessTask(1L, request1, eventStore, idGenerator);
  private final SagaTask task2 = new RequestProcessTask(2L, request2, eventStore, idGenerator);
  private final SagaTask task3 = new RequestProcessTask(3L, request3, eventStore, idGenerator);
  private final SagaTask task4 = new RequestProcessTask(4L, request4, eventStore, idGenerator);
  private final SagaTask sagaEndTask = new SagaEndTask(5L, eventStore, idGenerator);

  private final RuntimeException exception = new RuntimeException("oops");

  private final Node<SagaTask> node1 = new Node<>(task1.id(), task1);
  private final Node<SagaTask> node2 = new Node<>(task2.id(), task2);
  private final Node<SagaTask> node3 = new Node<>(task3.id(), task3);
  private final Node<SagaTask> root = new Node<>(sagaStartTask.id(), sagaStartTask);
  private final Node<SagaTask> leaf = new Node<>(sagaEndTask.id(), sagaEndTask);
  private final SingleLeafDirectedAcyclicGraph<SagaTask> sagaTaskGraph = new SingleLeafDirectedAcyclicGraph<>(root, leaf);

  private Saga saga;

  // root - node1 - node2 - node3 - leaf
  @Before
  public void setUp() throws Exception {
    root.addChild(node1);
    node1.addChild(node2);
    node2.addChild(node3);
    node3.addChild(leaf);

    saga = new Saga(idGenerator, eventStore, sagaTaskGraph);
  }

  @Test
  public void transactionsAreRunSuccessfully() {
    saga.run();

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

    for (Transaction transaction : transactions) {
      verify(transaction).run();
    }

    for (Compensation compensation : compensations) {
      verify(compensation, never()).run();
    }
  }

  @Test
  public void compensateCommittedTransactionsOnFailure() {
    doThrow(exception).when(transaction2).run();

    saga.run();

    assertThat(eventStore, contains(
        eventWith(1L, NO_OP_TRANSACTION, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionAbortedEvent.class),
        eventWith(6L, compensation1, CompensationStartedEvent.class),
        eventWith(7L, compensation1, CompensationEndedEvent.class),
        eventWith(8L, NO_OP_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).run();
    verify(transaction2).run();
    verify(transaction3, never()).run();

    verify(compensation1).run();
    verify(compensation3, never()).run();
  }

  // root - node1 - node2 - node3 - leaf
  //             \_ node4 _/
  @Ignore // this may fail from time to time, because event ordering is not consistent due to tasks running in parallel
  @Test
  public void redoHangingTransactionsOnFailure() throws InterruptedException {
    Node<SagaTask> node4 = new Node<>(task4.id(), task4);
    node1.addChild(node4);
    node4.addChild(node3);

    doThrow(exception).when(transaction4).run();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(withAnswer(() -> {
      latch.await();
      return null;
    })).doNothing().when(transaction2).run();

    executor.execute(saga::run);

    waitTillSlowTransactionStarted(transaction2, transaction4);

    // the ordering of events may not be consistence due to concurrent processing of requests
    await().atMost(5, SECONDS).until(() -> {
      assertThat(eventStore, contains(
          eventWith(1L, NO_OP_TRANSACTION, SagaStartedEvent.class),
          eventWith(2L, transaction1, TransactionStartedEvent.class),
          eventWith(3L, transaction1, TransactionEndedEvent.class),
          anyOf(
              eventWith(4L, transaction2, TransactionStartedEvent.class),
              eventWith(4L, transaction4, TransactionStartedEvent.class)),
          anyOf(
              eventWith(5L, transaction4, TransactionStartedEvent.class),
              eventWith(5L, transaction2, TransactionStartedEvent.class),
              eventWith(5L, transaction4, TransactionAbortedEvent.class)),
          anyOf(
              eventWith(6L, transaction4, TransactionAbortedEvent.class),
              eventWith(6L, transaction4, TransactionStartedEvent.class)),
          eventWith(7L, transaction2, TransactionStartedEvent.class),
          eventWith(8L, transaction2, TransactionEndedEvent.class),
          eventWith(9L, compensation2, CompensationStartedEvent.class),
          eventWith(10L, compensation2, CompensationEndedEvent.class),
          eventWith(11L, compensation1, CompensationStartedEvent.class),
          eventWith(12L, compensation1, CompensationEndedEvent.class),
          eventWith(13L, NO_OP_COMPENSATION, SagaEndedEvent.class)));
      return true;
    });

    verify(transaction1).run();
    verify(transaction2, times(2)).run();
    verify(transaction3, never()).run();

    verify(compensation1).run();
    verify(compensation2).run();
    verify(compensation3, never()).run();

    latch.countDown();

    executor.shutdown();
  }

  @Test
  public void retriesFailedTransactionTillSuccess() {
    Saga saga = new Saga(idGenerator, eventStore, new ForwardRecovery(), sagaTaskGraph);

    doThrow(exception).doThrow(exception).doNothing().when(transaction2).run();

    saga.run();

    assertThat(eventStore, contains(
        eventWith(1L, NO_OP_TRANSACTION, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionStartedEvent.class),
        eventWith(6L, transaction2, TransactionStartedEvent.class),
        eventWith(7L, transaction2, TransactionEndedEvent.class),
        eventWith(8L, transaction3, TransactionStartedEvent.class),
        eventWith(9L, transaction3, TransactionEndedEvent.class),
        eventWith(10L, NO_OP_COMPENSATION, SagaEndedEvent.class)
    ));

    for (Transaction transaction : transactions) {
      verify(transaction, atLeastOnce()).run();
    }

    for (Compensation compensation : compensations) {
      verify(compensation, never()).run();
    }

    verify(transaction2, times(3)).run();
  }

  @Test
  public void restoresSagaToTransactionStateByPlayingAllEvents() {
    Iterable<SagaEvent> events = asList(
        new SagaStartedEvent(1L, sagaStartTask),
        new TransactionStartedEvent(2L, task1),
        new TransactionEndedEvent(3L, task1),
        new TransactionStartedEvent(4L, task2)
    );

    saga.play(events);

    saga.run();
    assertThat(eventStore, contains(
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionEndedEvent.class),
        eventWith(6L, transaction3, TransactionStartedEvent.class),
        eventWith(7L, transaction3, TransactionEndedEvent.class),
        eventWith(8L, NO_OP_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).run();
    verify(transaction2).run();
    verify(transaction3).run();

    for (Compensation compensation : compensations) {
      verify(compensation, never()).run();
    }
  }

  @Test
  public void restoresSagaToCompensationStateByPlayingAllEvents() {
    Iterable<SagaEvent> events = asList(
        new SagaStartedEvent(1L, sagaStartTask),
        new TransactionStartedEvent(2L, task1),
        new TransactionEndedEvent(3L, task1),
        new TransactionStartedEvent(4L, task2),
        new TransactionEndedEvent(5L, task2),
        new CompensationStartedEvent(6L, task2),
        new CompensationEndedEvent(7L, task2),
        new CompensationStartedEvent(8L, task1)
    );

    saga.play(events);

    saga.run();
    assertThat(eventStore, contains(
        eventWith(8L, compensation1, CompensationStartedEvent.class),
        eventWith(9L, compensation1, CompensationEndedEvent.class),
        eventWith(10L, NO_OP_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).run();
    verify(transaction2, never()).run();
    verify(transaction3, never()).run();

    verify(compensation1).run();
    verify(compensation2, never()).run();
    verify(compensation3, never()).run();
  }

  @Test
  public void restoresSagaToEndStateByPlayingAllEvents() {
    Iterable<SagaEvent> events = asList(
        new SagaStartedEvent(1L, sagaStartTask),
        new TransactionStartedEvent(2L, task1),
        new TransactionEndedEvent(3L, task1),
        new TransactionStartedEvent(4L, task2),
        new TransactionEndedEvent(5L, task2),
        new TransactionStartedEvent(6L, task3),
        new TransactionEndedEvent(7L, task3)
    );

    saga.play(events);

    saga.run();
    assertThat(eventStore, contains(
        eventWith(8L, NO_OP_COMPENSATION, SagaEndedEvent.class)
    ));

    for (Transaction transaction : transactions) {
      verify(transaction, never()).run();
    }

    for (Compensation compensation : compensations) {
      verify(compensation, never()).run();
    }
  }

  private void waitTillSlowTransactionStarted(Transaction... transactions) {
    await().atMost(5, SECONDS).until(() -> {
      for (Transaction transaction : transactions) {
        verify(transaction, atLeastOnce()).run();
      }
      return true;
    });
  }

  private Answer<Void> withAnswer(Callable<Void> callable) {
    return invocationOnMock -> callable.call();
  }
}
