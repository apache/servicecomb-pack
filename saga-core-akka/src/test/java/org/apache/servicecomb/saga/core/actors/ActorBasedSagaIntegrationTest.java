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

package org.apache.servicecomb.saga.core.actors;

import static org.apache.servicecomb.saga.core.Transaction.SAGA_END_TRANSACTION;
import static org.apache.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.apache.servicecomb.saga.core.BackwardRecovery;
import org.apache.servicecomb.saga.core.Compensation;
import org.apache.servicecomb.saga.core.EventEnvelope;
import org.apache.servicecomb.saga.core.EventStore;
import org.apache.servicecomb.saga.core.Fallback;
import org.apache.servicecomb.saga.core.ForwardRecovery;
import org.apache.servicecomb.saga.core.IdGenerator;
import org.apache.servicecomb.saga.core.LongIdGenerator;
import org.apache.servicecomb.saga.core.NoOpSagaRequest;
import org.apache.servicecomb.saga.core.Operation;
import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.Saga;
import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaEndedEvent;
import org.apache.servicecomb.saga.core.SagaEvent;
import org.apache.servicecomb.saga.core.SagaEventMatcher;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaRequestImpl;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SagaStartedEvent;
import org.apache.servicecomb.saga.core.SuccessfulSagaResponse;
import org.apache.servicecomb.saga.core.Transaction;
import org.apache.servicecomb.saga.core.TransactionAbortedEvent;
import org.apache.servicecomb.saga.core.TransactionCompensatedEvent;
import org.apache.servicecomb.saga.core.TransactionEndedEvent;
import org.apache.servicecomb.saga.core.TransactionStartedEvent;
import org.apache.servicecomb.saga.core.application.SagaFactory;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import com.seanyinx.github.unit.scaffolding.Randomness;

import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import org.apache.servicecomb.saga.infrastructure.EmbeddedEventStore;

@SuppressWarnings("unchecked")
public class ActorBasedSagaIntegrationTest {
  private static final String sagaId = Randomness.uniquify("sagaId");

  private final FromJsonFormat<Set<String>> childrenExtractor = mock(FromJsonFormat.class);
  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventStore eventStore = new EmbeddedEventStore();

  private final PersistentStore persistentStore = mock(PersistentStore.class);
  private final SagaDefinition sagaDefinition = mock(SagaDefinition.class);

  private final Transaction transaction1 = mock(Transaction.class, "transaction1");
  private final Transaction transaction2 = mock(Transaction.class, "transaction2");
  private final Transaction transaction3 = mock(Transaction.class, "transaction3");
  private final Transaction transaction4 = mock(Transaction.class, "transaction4");

  private final Compensation compensation1 = mock(Compensation.class, "compensation1");
  private final Compensation compensation2 = mock(Compensation.class, "compensation2");
  private final Compensation compensation3 = mock(Compensation.class, "compensation3");
  private final Compensation compensation4 = mock(Compensation.class, "compensation4");

  private final Fallback fallback1 = mock(Fallback.class, "fallback1");

  private final String requestJson = "{}";
  private final SagaRequest request1 = request("request1", "service1", transaction1, compensation1, fallback1);
  private final SagaRequest request2 = request("request2", "service2", transaction2, compensation2, request1.id());
  private final SagaRequest request3 = request("request3", "service3", transaction3, compensation3, request1.id());
  private final SagaRequest request4 = request("request4", "service4", transaction4, compensation4, request3.id());

  private final SagaResponse transactionResponse1 = new SuccessfulSagaResponse("transaction1");
  private final SagaResponse transactionResponse2 = new SuccessfulSagaResponse("transaction2");
  private final SagaResponse transactionResponse3 = new SuccessfulSagaResponse("transaction3");
  private final SagaResponse compensationResponse1 = new SuccessfulSagaResponse("compensation1");
  private final SagaResponse compensationResponse2 = new SuccessfulSagaResponse("compensation2");
  private final SagaResponse compensationResponse3 = new SuccessfulSagaResponse("compensation3");

  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private final RuntimeException exception = new RuntimeException("oops");

  private Saga saga;
  private final SagaFactory sagaFactory = new ActorBasedSagaFactory(100, persistentStore, childrenExtractor);

  // root - node1 - node2 - leaf
  @Before
  public void setUp() throws Exception {
    when(sagaDefinition.policy()).thenReturn(new BackwardRecovery());
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2});

    when(childrenExtractor.fromJson(anyString())).thenReturn(emptySet());
    when(childrenExtractor.fromJson(SagaResponse.NONE_RESPONSE.body())).thenReturn(setOf("none"));

    when(transaction1.send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE)).thenReturn(transactionResponse1);
    when(transaction2.send(request2.serviceName(), transactionResponse1)).thenReturn(transactionResponse2);
    when(transaction3.send(request3.serviceName(), transactionResponse1)).thenReturn(transactionResponse3);

    when(compensation1.send(request1.serviceName(), compensationResponse2)).thenReturn(compensationResponse1);
    when(compensation2.send(request2.serviceName(), compensationResponse3)).thenReturn(compensationResponse2);
    when(compensation3.send(request3.serviceName(), SagaResponse.EMPTY_RESPONSE)).thenReturn(compensationResponse3);
  }

  @After
  public void tearDown() throws Exception {
    sagaFactory.terminate();
    assertTrue(sagaFactory.isTerminated());
  }

  @Test
  public void transactionsAreRunSuccessfully() {
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);
    saga.run();

    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction2).send(request2.serviceName(), transactionResponse1);

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
  }

  // root - node1 - node2 - leaf
  //             \_ node3 _/
  @Test
  public void compensateCommittedTransactionsOnFailure() {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    // barrier to make sure the two transactions starts at the same time
    CyclicBarrier barrier = new CyclicBarrier(2);
    when(transaction2.send(request2.serviceName(), transactionResponse1))
        .thenAnswer(
            withAnswer(() -> {
              barrier.await();
              Thread.sleep(100);
              throw exception;
            }));

    when(transaction3.send(request3.serviceName(), transactionResponse1))
        .thenAnswer(
            withAnswer(() -> {
              barrier.await();
              return transactionResponse3;
            }));

    saga.run();

    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        CoreMatchers.anyOf(SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class), SagaEventMatcher
            .eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        CoreMatchers.anyOf(SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class), SagaEventMatcher
            .eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionAbortedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation3, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, Compensation.SAGA_START_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction2).send(request2.serviceName(), transactionResponse1);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3).send(request3.serviceName());
  }

  @Test
  public void skipIgnoredTransaction() throws Exception {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    when(childrenExtractor.fromJson(transactionResponse1.body())).thenReturn(setOf(request3.id()));

    saga.run();

    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void skipAllIgnoredTransactions() throws Exception {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3, request4});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    when(childrenExtractor.fromJson(transactionResponse1.body())).thenReturn(setOf("none"));

    saga.run();

    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction4, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
    verify(compensation4, never()).send(request4.serviceName());
  }

  @Test
  public void doNotCompensateIgnoredTransactions() throws Exception {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3, request4});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    when(childrenExtractor.fromJson(transactionResponse1.body())).thenReturn(setOf(request3.id()));

    when(transaction4.send(request4.serviceName(), transactionResponse3)).thenThrow(exception);

    saga.run();

    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction4, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction4, TransactionAbortedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, Compensation.SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);
    verify(transaction4).send(request4.serviceName(), transactionResponse3);
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1).send(request1.serviceName());
    verify(compensation3).send(request3.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation4, never()).send(request4.serviceName());
  }

  // TODO: 2017/10/31 actor will hang and its parent and children will be blocked without its response, timeout must be applied
  @Ignore
  // root - node1 - node2 - leaf
  //             \_ node3 _/
  @Test
  public void redoHangingTransactionsOnFailure() throws InterruptedException {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    // barrier to make sure the two transactions starts at the same time
    CyclicBarrier barrier = new CyclicBarrier(2);
    when(transaction3.send(request3.serviceName(), transactionResponse1))
        .thenAnswer(withAnswer(() -> {
      barrier.await();
      throw exception;
    }));

    CountDownLatch latch = new CountDownLatch(1);

    when(transaction2.send(request2.serviceName(), transactionResponse1))
        .thenAnswer(withAnswer(() -> {
      barrier.await();
      latch.await(1, SECONDS);
      return transactionResponse2;
    })).thenReturn(transactionResponse2);

    saga.run();

    // the ordering of events may not be consistence due to concurrent processing of requests
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        CoreMatchers.anyOf(
            SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
            SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        CoreMatchers.anyOf(
            SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
            SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class)),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionAbortedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, Compensation.SAGA_START_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction2, times(2)).send(request2.serviceName(), transactionResponse1);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

    verify(compensation1).send(request1.serviceName());
    verify(compensation2).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());

    latch.countDown();
  }

  @Test
  public void retriesFailedTransactionTillSuccess() {
    when(sagaDefinition.policy()).thenReturn(new ForwardRecovery());
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    when(transaction2.send(request2.serviceName(), transactionResponse1))
        .thenThrow(exception).thenThrow(exception).thenReturn(transactionResponse2);
    when(transaction2.retries()).thenReturn(-1);

    saga.run();

    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction2, times(3)).send(request2.serviceName(), transactionResponse1);

    verify(compensation1, never()).send(anyString(), any(SagaResponse.class));
    verify(compensation2, never()).send(anyString(), any(SagaResponse.class));
  }

  @Test
  public void fallbackWhenCompensationFailed() {
    int retries = 3;
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    when(transaction2.send(request2.serviceName(), transactionResponse1)).thenThrow(exception);
    when(compensation1.send(request1.serviceName())).thenThrow(exception);
    when(compensation1.retries()).thenReturn(retries);

    saga.run();

    verify(transaction1).send(request1.serviceName(), SagaResponse.EMPTY_RESPONSE);
    verify(transaction2).send(request2.serviceName(), transactionResponse1);

    verify(compensation1, times(retries + 1)).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());

    verify(fallback1).send(request1.serviceName());
  }

  @Test
  public void restoresSagaToTransactionStateByPlayingAllEvents() {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, NoOpSagaRequest.SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1, transactionResponse1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2, transactionResponse2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresPartialTransactionByPlayingAllEvents() {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, NoOpSagaRequest.SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1, transactionResponse1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2, transactionResponse2)),
        envelope(new TransactionStartedEvent(sagaId, request3))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresToCompensationFromAbortedTransactionByPlayingAllEvents() {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, NoOpSagaRequest.SAGA_START_REQUEST)),
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
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionAbortedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, Compensation.SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1).send(request1.serviceName());
    verify(compensation2).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresSagaToCompensationStateByPlayingAllEvents() {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, NoOpSagaRequest.SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3)),
        envelope(new TransactionAbortedEvent(sagaId, request3, exception)),
        envelope(new TransactionCompensatedEvent(sagaId, request2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionAbortedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, Compensation.SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresPartialCompensationByPlayingAllEvents() {
    when(sagaDefinition.requests()).thenReturn(new SagaRequest[]{request1, request2, request3});
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, NoOpSagaRequest.SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3)),
        envelope(new TransactionAbortedEvent(sagaId, request3, exception)),
        envelope(new TransactionCompensatedEvent(sagaId, request2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction3, TransactionAbortedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        SagaEventMatcher.eventWith(sagaId, Compensation.SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void restoresSagaToEndStateByPlayingAllEvents() {
    saga = sagaFactory.createSaga(requestJson, sagaId, eventStore, sagaDefinition);
    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, NoOpSagaRequest.SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2))
    );

    eventStore.populate(events);
    saga.play();

    saga.run();
    assertThat(eventStore, IsIterableContainingInOrder.contains(
        SagaEventMatcher.eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        SagaEventMatcher.eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        SagaEventMatcher.eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
  }

  @Test
  public void failFastIfSagaLogIsDown() throws Exception {
    EventStore sagaLog = mock(EventStore.class);
    saga = sagaFactory.createSaga(requestJson, sagaId, sagaLog, sagaDefinition);

    doThrow(RuntimeException.class).when(sagaLog).offer(any(SagaStartedEvent.class));

    saga.run();

    verify(sagaLog).offer(any(SagaStartedEvent.class));
    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
  }

  private Answer<SagaResponse> withAnswer(Callable<SagaResponse> callable) {
    return invocationOnMock -> callable.call();
  }

  private EventEnvelope envelope(SagaEvent event) {
    return new EventEnvelope(idGenerator.nextId(), event);
  }

  private SagaRequest request(String requestId,
      String serviceName,
      Transaction transaction,
      Compensation compensation,
      String... parentIds) {

    return new SagaRequestImpl(requestId, serviceName, Operation.TYPE_REST, transaction, compensation, parentIds);
  }

  private SagaRequest request(String requestId,
      String serviceName,
      Transaction transaction,
      Compensation compensation,
      Fallback fallback) {

    return new SagaRequestImpl(requestId, serviceName, Operation.TYPE_REST, transaction, compensation, fallback);
  }

  private HashSet<String> setOf(String requestId) {
    return new HashSet<>(singletonList(requestId));
  }
}
