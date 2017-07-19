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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

public class Saga {

  private final IdGenerator<Long> idGenerator;
  private final EventQueue eventQueue;
  private final SagaRequest[] requests;
  private final RecoveryPolicy recoveryPolicy;

  private volatile SagaState currentState;

  public Saga(IdGenerator<Long> idGenerator, EventQueue eventQueue, SagaRequest... requests) {
    this(idGenerator, eventQueue, new BackwardRecovery(), requests);
  }

  public Saga(IdGenerator<Long> idGenerator, EventQueue eventQueue, RecoveryPolicy recoveryPolicy,
      SagaRequest... requests) {

    this.idGenerator = idGenerator;
    this.eventQueue = eventQueue;
    this.requests = requests;
    this.recoveryPolicy = recoveryPolicy;
  }

  public void run() {
    Deque<SagaTask> executedTasks = new LinkedList<>();
    Queue<SagaTask> pendingTasks = populatePendingSagaTasks();

    currentState = TransactionState.INSTANCE;
    do {
      try {
        currentState.invoke(executedTasks, pendingTasks);
      } catch (Exception e) {
        currentState = recoveryPolicy.apply(currentState);
      }
    } while (!pendingTasks.isEmpty() && !executedTasks.isEmpty());
  }

  public void abort() {
    currentState = recoveryPolicy.apply(currentState);
    new SagaAbortTask(eventQueue, idGenerator).commit();
  }

  private Queue<SagaTask> populatePendingSagaTasks() {
    Queue<SagaTask> pendingTasks = new LinkedList<>();

    pendingTasks.add(new SagaStartTask(eventQueue, idGenerator));

    for (SagaRequest request : requests) {
      pendingTasks.add(new RequestProcessTask(request, eventQueue, idGenerator));
    }

    pendingTasks.add(new SagaEndTask(eventQueue, idGenerator));
    return pendingTasks;
  }
}
