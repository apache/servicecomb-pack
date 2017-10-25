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
import static io.servicecomb.saga.core.Operation.TYPE_REST;
import static io.servicecomb.saga.core.SagaResponse.NONE_RESPONSE;
import static io.servicecomb.saga.core.SagaEventMatcher.eventWith;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static io.servicecomb.saga.core.SagaTask.SAGA_END_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_REQUEST_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_START_TASK;
import static io.servicecomb.saga.core.Transaction.SAGA_END_TRANSACTION;
import static io.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import com.seanyinx.github.unit.scaffolding.Randomness;

import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;

@SuppressWarnings("unchecked")
public class SagaIntegrationTest {
  private static final String sagaId = Randomness.uniquify("sagaId");

  private final FromJsonFormat<Set<String>> childrenExtractor = mock(FromJsonFormat.class);
  private final SagaContext sagaContext = new SagaContextImpl(childrenExtractor);
  private final IdGenerator<Long> idGenerator = new LongIdGenerator();
  private final EventStore eventStore = new EmbeddedEventStore(sagaContext);

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

  private final Node<SagaRequest> node1 = new Node<>(1, request1);
  private final Node<SagaRequest> node2 = new Node<>(2, request2);
  private final Node<SagaRequest> node3 = new Node<>(3, request3);
  private final Node<SagaRequest> node4 = new Node<>(4, request4);
  private final Node<SagaRequest> root = new Node<>(0, SAGA_START_REQUEST);
  private final Node<SagaRequest> leaf = new Node<>(5, SAGA_END_REQUEST);
  private final SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph = new SingleLeafDirectedAcyclicGraph<>(root, leaf);

  private Saga saga;
  private final Map<String, SagaTask> tasks = new HashMap<>();

  // root - node1 - node2 - leaf
  @Before
  public void setUp() throws Exception {
    when(childrenExtractor.fromJson(anyString())).thenReturn(emptySet());
    when(childrenExtractor.fromJson(NONE_RESPONSE.body())).thenReturn(setOf("none"));

    when(transaction1.send(request1.serviceName(), EMPTY_RESPONSE)).thenReturn(transactionResponse1);
    when(transaction2.send(request2.serviceName(), transactionResponse1)).thenReturn(transactionResponse2);
    when(transaction3.send(request3.serviceName(), transactionResponse1)).thenReturn(transactionResponse3);

    when(compensation1.send(request1.serviceName(), compensationResponse2)).thenReturn(compensationResponse1);
    when(compensation2.send(request2.serviceName(), compensationResponse3)).thenReturn(compensationResponse2);
    when(compensation3.send(request3.serviceName(), EMPTY_RESPONSE)).thenReturn(compensationResponse3);

    root.addChild(node1);
    node1.addChild(node2);
    node2.addChild(leaf);

    SagaStartTask sagaStartTask = new SagaStartTask(sagaId, requestJson, eventStore);
    SagaEndTask sagaEndTask = new SagaEndTask(sagaId, eventStore);
    RequestProcessTask processTask = requestProcessTask(new BackwardRecovery());

    tasks.put(SAGA_START_TASK, sagaStartTask);
    tasks.put(SAGA_REQUEST_TASK, processTask);
    tasks.put(SAGA_END_TASK, sagaEndTask);

    saga = new Saga(eventStore, tasks, sagaContext, sagaTaskGraph);
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

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction2).send(request2.serviceName(), transactionResponse1);

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

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        anyOf(eventWith(sagaId, transaction2, TransactionStartedEvent.class), eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        anyOf(eventWith(sagaId, transaction2, TransactionStartedEvent.class), eventWith(sagaId, transaction3, TransactionStartedEvent.class)),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionAbortedEvent.class),
        eventWith(sagaId, compensation3, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction2).send(request2.serviceName(), transactionResponse1);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

    verify(compensation1).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3).send(request3.serviceName());
  }

  @Test
  public void skipIgnoredTransaction() throws Exception {
    addExtraChildToNode1();

    when(childrenExtractor.fromJson(transactionResponse1.body())).thenReturn(setOf(request3.id()));

    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1, never()).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());
  }

  @Test
  public void skipAllIgnoredTransactions() throws Exception {
    node1.addChild(node3);
    node3.addChild(node4);
    node4.addChild(leaf);

    when(childrenExtractor.fromJson(transactionResponse1.body())).thenReturn(setOf("none"));

    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
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
    node1.addChild(node3);
    node3.addChild(node4);
    node4.addChild(leaf);

    when(childrenExtractor.fromJson(transactionResponse1.body())).thenReturn(setOf(request3.id()));

    when(transaction4.send(request4.serviceName(), transactionResponse3)).thenThrow(exception);

    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction3, TransactionStartedEvent.class),
        eventWith(sagaId, transaction3, TransactionEndedEvent.class),
        eventWith(sagaId, transaction4, TransactionStartedEvent.class),
        eventWith(sagaId, transaction4, TransactionAbortedEvent.class),
        eventWith(sagaId, transaction3, TransactionCompensatedEvent.class),
        eventWith(sagaId, transaction1, TransactionCompensatedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);
    verify(transaction4).send(request4.serviceName(), transactionResponse3);
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));

    verify(compensation1).send(request1.serviceName());
    verify(compensation3).send(request3.serviceName());
    verify(compensation2, never()).send(request2.serviceName());
    verify(compensation4, never()).send(request4.serviceName());
  }

  // root - node1 - node2 - leaf
  //             \_ node3 _/
  @Test
  public void redoHangingTransactionsOnFailure() throws InterruptedException {
    addExtraChildToNode1();

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
      latch.await();
      return transactionResponse2;
    })).thenReturn(transactionResponse2);

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
        eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)));

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction2, times(2)).send(request2.serviceName(), transactionResponse1);
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

    verify(compensation1).send(request1.serviceName());
    verify(compensation2).send(request2.serviceName());
    verify(compensation3, never()).send(request3.serviceName());

    latch.countDown();
  }

  @Test
  public void retriesFailedTransactionTillSuccess() {
    RequestProcessTask processTask = requestProcessTask(new ForwardRecovery());
    tasks.put(SAGA_REQUEST_TASK, processTask);

    when(transaction2.send(request2.serviceName(), transactionResponse1))
        .thenThrow(exception).thenThrow(exception).thenReturn(transactionResponse2);

    saga.run();

    assertThat(eventStore, contains(
        eventWith(sagaId, SAGA_START_TRANSACTION, SagaStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionStartedEvent.class),
        eventWith(sagaId, transaction1, TransactionEndedEvent.class),
        eventWith(sagaId, transaction2, TransactionStartedEvent.class),
        eventWith(sagaId, transaction2, TransactionEndedEvent.class),
        eventWith(sagaId, SAGA_END_TRANSACTION, SagaEndedEvent.class)
    ));

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction2, times(3)).send(request2.serviceName(), transactionResponse1);

    verify(compensation1, never()).send(anyString(), any(SagaResponse.class));
    verify(compensation2, never()).send(anyString(), any(SagaResponse.class));
  }

  @Test
  public void fallbackWhenCompensationFailed() {
    int retries = 3;

    when(transaction2.send(request2.serviceName(), transactionResponse1)).thenThrow(exception);
    when(compensation1.send(request1.serviceName())).thenThrow(exception);
    when(compensation1.retries()).thenReturn(retries);

    saga.run();

    verify(transaction1).send(request1.serviceName(), EMPTY_RESPONSE);
    verify(transaction2).send(request2.serviceName(), transactionResponse1);

    verify(compensation1, times(retries + 1)).send(request1.serviceName());
    verify(compensation2, never()).send(request2.serviceName());

    verify(fallback1).send(request1.serviceName());
  }

  @Test
  public void restoresSagaToTransactionStateByPlayingAllEvents() {
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1, transactionResponse1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2, transactionResponse2))
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

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

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
        envelope(new TransactionEndedEvent(sagaId, request1, transactionResponse1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2, transactionResponse2)),
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

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3).send(request3.serviceName(), transactionResponse1);

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
        eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
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
    addExtraChildToNode1();

    Iterable<EventEnvelope> events = asList(
        envelope(new SagaStartedEvent(sagaId, requestJson, SAGA_START_REQUEST)),
        envelope(new TransactionStartedEvent(sagaId, request1)),
        envelope(new TransactionEndedEvent(sagaId, request1)),
        envelope(new TransactionStartedEvent(sagaId, request2)),
        envelope(new TransactionEndedEvent(sagaId, request2)),
        envelope(new TransactionStartedEvent(sagaId, request3)),
        envelope(new TransactionEndedEvent(sagaId, request3)),
        envelope(new TransactionCompensatedEvent(sagaId, request2))
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
        eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation3, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3, never()).send(anyString(), any(SagaResponse.class));

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
        envelope(new TransactionCompensatedEvent(sagaId, request2))
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
        eventWith(sagaId, compensation2, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation3, TransactionCompensatedEvent.class),
        eventWith(sagaId, compensation1, TransactionCompensatedEvent.class),
        eventWith(sagaId, SAGA_START_COMPENSATION, SagaEndedEvent.class)
    ));

    verify(transaction1, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction2, never()).send(anyString(), any(SagaResponse.class));
    verify(transaction3, never()).send(anyString(), any(SagaResponse.class));

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

  private void addExtraChildToNode1() {
    node1.addChild(node3);
    node3.addChild(leaf);
  }

  private SagaRequest request(String requestId,
      String serviceName,
      Transaction transaction,
      Compensation compensation,
      String... parentIds) {

    return new SagaRequestImpl(requestId, serviceName, TYPE_REST, transaction, compensation, parentIds);
  }

  private SagaRequest request(String requestId,
      String serviceName,
      Transaction transaction,
      Compensation compensation,
      Fallback fallback) {

    return new SagaRequestImpl(requestId, serviceName, TYPE_REST, transaction, compensation, fallback);
  }

  private HashSet<String> setOf(String requestId) {
    return new HashSet<>(singletonList(requestId));
  }

  private RequestProcessTask requestProcessTask(RecoveryPolicy recoveryPolicy) {
    return new RequestProcessTask(sagaId, eventStore, recoveryPolicy, new FallbackPolicy(100));
  }
}
