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

import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.Traveller;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransactionState extends AbstractSagaState {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final IdGenerator<Long> idGenerator;
  private final CompletionService<Operation> executorService;
  private final RecoveryPolicy recoveryPolicy;

  TransactionState(
      IdGenerator<Long> idGenerator,
      CompletionService<Operation> executorService,
      RecoveryPolicy recoveryPolicy, Traveller<SagaTask> traveller) {

    super(traveller);

    this.idGenerator = idGenerator;
    this.executorService = executorService;
    this.recoveryPolicy = recoveryPolicy;
  }

  @Override
  public void invoke(Deque<SagaTask> executedTasks, Queue<SagaTask> pendingTasks) {
    SagaTask task = pendingTasks.peek();
    executedTasks.push(task);

    log.info("Starting task {} id={}", task.description(), task.id());
    try {
      task.commit();
    } catch (OperationTimeoutException e) {
      log.error("Retrying timed out Transaction", e);
      task.commit();
    }
    log.info("Completed task {} id={}", task.description(), task.id());

    pendingTasks.poll();
  }

  @Override
  void invoke(Collection<Node<SagaTask>> nodes) {
    Map<Future<Operation>, SagaTask> futures = new HashMap<>(nodes.size());
    for (Node<SagaTask> node : nodes) {
      SagaTask task = node.value();
      futures.put(futureOf(task), task);
    }

    for (int i = 0; i < futures.size(); i++) {
      try {
        Future<Operation> future = executorService.take();
        try {
          future.get();
        } catch (ExecutionException e) {
          throw new TransactionFailedException(e.getCause());
        }
      } catch (InterruptedException e) {
        throw new TransactionFailedException(e);
      }
    }
  }

  @Override
  boolean replay(Collection<Node<SagaTask>> nodes, Map<Operation, Collection<SagaEvent>> completedOperations) {

    for (Iterator<Node<SagaTask>> iterator = nodes.iterator(); iterator.hasNext(); ) {
      SagaTask task = iterator.next().value();
      if (completedOperations.containsKey(task.transaction())) {
        for (SagaEvent event : completedOperations.get(task.transaction())) {
          log.info("Start playing event {} id={}", event.description(), event.id());
          event.play(idGenerator, iterator);
          log.info("Completed playing event {} id={}", event.description(), event.id());
        }
      }
      completedOperations.remove(task.transaction());
    }
    return !nodes.isEmpty();
  }

  private Future<Operation> futureOf(SagaTask task) {
    return executorService.submit(() -> {
      recoveryPolicy.apply(task);
      return task.transaction();
    });
  }
}
