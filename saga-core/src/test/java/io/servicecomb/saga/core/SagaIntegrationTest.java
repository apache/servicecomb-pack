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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SagaIntegrationTest {

  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventQueue eventQueue = new EmbeddedEventQueue();

  private final Transaction transaction1 = mock(Transaction.class);
  private final Transaction transaction2 = mock(Transaction.class);
  private final Transaction transaction3 = mock(Transaction.class);

  private final Transaction[] transactions = {transaction1, transaction2, transaction3};

  private final Compensation compensation1 = mock(Compensation.class);
  private final Compensation compensation2 = mock(Compensation.class);
  private final Compensation compensation3 = mock(Compensation.class);

  private final Compensation[] compensations = {compensation1, compensation2, compensation3};

  private final SagaRequest request1 = new SagaRequest(transaction1, compensation1);
  private final SagaRequest request2 = new SagaRequest(transaction2, compensation2);
  private final SagaRequest request3 = new SagaRequest(transaction3, compensation3);

  private final SagaRequest[] requests = {request1, request2, request3};

  private final Saga saga = new Saga(idGenerator, eventQueue, requests);

  @Test
  public void transactionsAreRunSuccessfully() {
    saga.run();

    assertThat(eventQueue, contains(
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
    doThrow(new RuntimeException("oops")).when(transaction2).run();

    saga.run();

    assertThat(eventQueue, contains(
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
    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
        latch.await();
        return null;
      }
    }).when(transaction2).run();

    executor.execute(saga::run);

    await().atMost(1, SECONDS).until(() -> {
      verify(transaction2).run();
      return true;
    });

    saga.abort();
    latch.countDown();

    await().atMost(1, SECONDS).until(() -> {
      assertThat(eventQueue, contains(
          eventWith(1L, NO_OP, SagaStartedEvent.class),
          eventWith(2L, transaction1, TransactionStartedEvent.class),
          eventWith(3L, transaction1, TransactionEndedEvent.class),
          eventWith(4L, transaction2, TransactionStartedEvent.class),
          eventWith(5L, NO_OP, SagaAbortedEvent.class),
          eventWith(6L, transaction2, TransactionEndedEvent.class),
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
}
