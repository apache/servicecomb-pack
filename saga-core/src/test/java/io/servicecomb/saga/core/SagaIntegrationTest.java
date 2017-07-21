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

import static io.servicecomb.saga.core.Operation.NO_OP;
import static io.servicecomb.saga.core.SagaEventMatcher.eventWith;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class SagaIntegrationTest {

  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventStore eventStore = new EmbeddedEventStore();

  private final Transaction transaction1 = mock(Transaction.class, "transaction1");
  private final Transaction transaction2 = mock(Transaction.class, "transaction2");
  private final Transaction transaction3 = mock(Transaction.class, "transaction3");

  private final Transaction[] transactions = {transaction1, transaction2, transaction3};

  private final Compensation compensation1 = mock(Compensation.class, "compensation1");
  private final Compensation compensation2 = mock(Compensation.class, "compensation2");
  private final Compensation compensation3 = mock(Compensation.class, "compensation3");

  private final Compensation[] compensations = {compensation1, compensation2, compensation3};

  private final SagaRequest request1 = new SagaRequest(transaction1, compensation1);
  private final SagaRequest request2 = new SagaRequest(transaction2, compensation2);
  private final SagaRequest request3 = new SagaRequest(transaction3, compensation3);

  private final SagaRequest[] requests = {request1, request2, request3};

  private final RuntimeException exception = new RuntimeException("oops");
  private final Saga saga = new Saga(idGenerator, eventStore, requests);

  @Test
  public void transactionsAreRunSuccessfully() {
    saga.run();

    assertThat(eventStore, contains(
        eventWith(1L, NO_OP, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionEndedEvent.class),
        eventWith(6L, transaction3, TransactionStartedEvent.class),
        eventWith(7L, transaction3, TransactionEndedEvent.class),
        eventWith(8L, NO_OP, SagaEndedEvent.class)
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
        eventWith(1L, NO_OP, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, compensation2, CompensationStartedEvent.class),
        eventWith(6L, compensation2, CompensationEndedEvent.class),
        eventWith(7L, compensation1, CompensationStartedEvent.class),
        eventWith(8L, compensation1, CompensationEndedEvent.class),
        eventWith(9L, NO_OP, SagaEndedEvent.class)));

    verify(transaction1).run();
    verify(transaction2).run();
    verify(transaction3, never()).run();

    verify(compensation1).run();
    verify(compensation2).run();
    verify(compensation3, never()).run();
  }

  @Test
  public void compensateCommittedTransactionsOnAbort() throws InterruptedException {
    Saga saga = new Saga(idGenerator, eventStore, new ForwardRecovery(), requests);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(withAnswer(() -> {
      latch.await();
      return null;
    })).when(transaction2).run();

    executor.execute(saga::run);

    waitTillSlowTransactionStarted(transaction2);

    saga.abort();
    latch.countDown();

    await().atMost(1, SECONDS).until(() -> {
      assertThat(eventStore, contains(
          eventWith(1L, NO_OP, SagaStartedEvent.class),
          eventWith(2L, transaction1, TransactionStartedEvent.class),
          eventWith(3L, transaction1, TransactionEndedEvent.class),
          eventWith(4L, transaction2, TransactionStartedEvent.class),
          eventWith(5L, transaction2, TransactionEndedEvent.class),
          eventWith(6L, NO_OP, SagaAbortedEvent.class),
          eventWith(7L, compensation2, CompensationStartedEvent.class),
          eventWith(8L, compensation2, CompensationEndedEvent.class),
          eventWith(9L, compensation1, CompensationStartedEvent.class),
          eventWith(10L, compensation1, CompensationEndedEvent.class),
          eventWith(11L, NO_OP, SagaEndedEvent.class)));
      return true;
    });

    verify(transaction1).run();
    verify(transaction2).run();
    verify(transaction3, never()).run();

    verify(compensation1).run();
    verify(compensation2).run();
    verify(compensation3, never()).run();

    executor.shutdown();
  }

  @Test
  public void retriesFailedTransactionsOnAbort() throws InterruptedException {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(withAnswer(() -> {
      latch.await();
      throw new OperationTimeoutException();
    })).doNothing().when(transaction2).run();

    executor.execute(saga::run);

    waitTillSlowTransactionStarted(transaction2);

    saga.abort();
    latch.countDown();

    await().atMost(1, SECONDS).until(() -> {
      assertThat(eventStore, contains(
          eventWith(1L, NO_OP, SagaStartedEvent.class),
          eventWith(2L, transaction1, TransactionStartedEvent.class),
          eventWith(3L, transaction1, TransactionEndedEvent.class),
          eventWith(4L, transaction2, TransactionStartedEvent.class),
          eventWith(5L, transaction2, TransactionStartedEvent.class),
          eventWith(6L, transaction2, TransactionEndedEvent.class),
          eventWith(7L, NO_OP, SagaAbortedEvent.class),
          eventWith(8L, compensation2, CompensationStartedEvent.class),
          eventWith(9L, compensation2, CompensationEndedEvent.class),
          eventWith(10L, compensation1, CompensationStartedEvent.class),
          eventWith(11L, compensation1, CompensationEndedEvent.class),
          eventWith(12L, NO_OP, SagaEndedEvent.class)));
      return true;
    });

    verify(transaction1).run();
    verify(transaction2, times(2)).run();
    verify(transaction3, never()).run();

    verify(compensation1).run();
    verify(compensation2).run();
    verify(compensation3, never()).run();

    executor.shutdown();
  }

  @Test
  public void retriesFailedTransactionTillSuccess() {
    Saga saga = new Saga(idGenerator, eventStore, new ForwardRecovery(), requests);

    doThrow(exception).doThrow(exception).doNothing().when(transaction2).run();

    saga.run();

    assertThat(eventStore, contains(
        eventWith(1L, NO_OP, SagaStartedEvent.class),
        eventWith(2L, transaction1, TransactionStartedEvent.class),
        eventWith(3L, transaction1, TransactionEndedEvent.class),
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionStartedEvent.class),
        eventWith(6L, transaction2, TransactionStartedEvent.class),
        eventWith(7L, transaction2, TransactionEndedEvent.class),
        eventWith(8L, transaction3, TransactionStartedEvent.class),
        eventWith(9L, transaction3, TransactionEndedEvent.class),
        eventWith(10L, NO_OP, SagaEndedEvent.class)
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
    Saga saga = new Saga(idGenerator, eventStore, requests);

    Iterable<SagaEvent> events = asList(
        new SagaStartedEvent(1L),
        new TransactionStartedEvent(2L, transaction1),
        new TransactionEndedEvent(3L, transaction1),
        new TransactionStartedEvent(4L, transaction2)
    );

    saga.play(events);

    saga.run();
    assertThat(eventStore, contains(
        eventWith(4L, transaction2, TransactionStartedEvent.class),
        eventWith(5L, transaction2, TransactionEndedEvent.class),
        eventWith(6L, transaction3, TransactionStartedEvent.class),
        eventWith(7L, transaction3, TransactionEndedEvent.class),
        eventWith(8L, NO_OP, SagaEndedEvent.class)
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
    Saga saga = new Saga(idGenerator, eventStore, requests);

    Iterable<SagaEvent> events = asList(
        new SagaStartedEvent(1L),
        new TransactionStartedEvent(2L, transaction1),
        new TransactionEndedEvent(3L, transaction1),
        new TransactionStartedEvent(4L, transaction2),
        new TransactionEndedEvent(5L, transaction2),
        new CompensationStartedEvent(6L, compensation2),
        new CompensationEndedEvent(7L, compensation2),
        new CompensationStartedEvent(8L, compensation1)
    );

    saga.play(events);

    saga.run();
    assertThat(eventStore, contains(
        eventWith(8L, compensation1, CompensationStartedEvent.class),
        eventWith(9L, compensation1, CompensationEndedEvent.class),
        eventWith(10L, NO_OP, SagaEndedEvent.class)
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
    Saga saga = new Saga(idGenerator, eventStore, requests);

    Iterable<SagaEvent> events = asList(
        new SagaStartedEvent(1L),
        new TransactionStartedEvent(2L, transaction1),
        new TransactionEndedEvent(3L, transaction1),
        new TransactionStartedEvent(4L, transaction2),
        new TransactionEndedEvent(5L, transaction2),
        new TransactionStartedEvent(6L, transaction3),
        new TransactionEndedEvent(7L, transaction3),
        new SagaEndedEvent(8L)
    );

    saga.play(events);

    saga.run();
    assertThat(eventStore.size(), is(0));

    for (Transaction transaction : transactions) {
      verify(transaction, never()).run();
    }

    for (Compensation compensation : compensations) {
      verify(compensation, never()).run();
    }
  }

  private void waitTillSlowTransactionStarted(Transaction transaction) {
    await().atMost(1, SECONDS).until(() -> {
      verify(transaction).run();
      return true;
    });
  }

  private Answer<Void> withAnswer(Callable<Void> callable) {
    return invocationOnMock -> callable.call();
  }
}
