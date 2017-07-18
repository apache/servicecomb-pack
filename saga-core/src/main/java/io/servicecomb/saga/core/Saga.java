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

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class Saga {

  private final IdGenerator<Long> idGenerator;
  private final EventQueue eventQueue;
  private final SagaRequest[] requests;

  private final SagaState compensationState;
  private final SagaState transactionState;

  public Saga(IdGenerator<Long> idGenerator, EventQueue eventQueue, SagaRequest[] requests) {
    this.idGenerator = idGenerator;
    this.eventQueue = eventQueue;
    this.requests = requests;
    this.compensationState = new CompensationState(idGenerator, eventQueue);
    this.transactionState = new TransactionState(eventQueue, idGenerator);
  }

  public void run() {
    Deque<SagaRequest> executedRequests = new LinkedList<>();
    Queue<SagaRequest> pendingRequests = new LinkedList<>();
    Collections.addAll(pendingRequests, requests);

    eventQueue.offer(new SagaStartedEvent(idGenerator.nextId()));

    try {
      transactionState.invoke(executedRequests, pendingRequests);
    } catch (Exception e) {
      compensationState.invoke(executedRequests, pendingRequests);
    }

    eventQueue.offer(new SagaEndedEvent(idGenerator.nextId()));
  }

  private static class CompensationState implements SagaState {

    private final IdGenerator<Long> idGenerator;
    private final EventQueue eventQueue;

    CompensationState(IdGenerator<Long> idGenerator, EventQueue eventQueue) {
      this.idGenerator = idGenerator;
      this.eventQueue = eventQueue;
    }

    @Override
    public void invoke(Deque<SagaRequest> executedRequests, Queue<SagaRequest> pendingRequests) {
      while (executedRequests.peek() != null) {
        SagaRequest executedRequest = executedRequests.pop();

        eventQueue.offer(new CompensationStartedEvent(idGenerator.nextId(), executedRequest.compensation()));
        executedRequest.abort();
        eventQueue.offer(new CompensationEndedEvent(idGenerator.nextId(), executedRequest.compensation()));
      }
    }
  }

  private static class TransactionState implements SagaState {

    private final EventQueue eventQueue;
    private final IdGenerator<Long> idGenerator;

    TransactionState(EventQueue eventQueue, IdGenerator<Long> idGenerator) {
      this.eventQueue = eventQueue;
      this.idGenerator = idGenerator;
    }


    public void invoke(Deque<SagaRequest> executedRequests, Queue<SagaRequest> pendingRequests) {
      while (pendingRequests.peek() != null) {
        SagaRequest request = pendingRequests.poll();
        executedRequests.push(request);

        eventQueue.offer(new TransactionStartedEvent(idGenerator.nextId(), request.transaction()));
        request.commit();
        eventQueue.offer(new TransactionEndedEvent(idGenerator.nextId(), request.transaction()));
      }
    }
  }
}
