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

import io.servicecomb.saga.core.dag.ByLevelTraveller;
import io.servicecomb.saga.core.dag.FromLeafTraversalDirection;
import io.servicecomb.saga.core.dag.FromRootTraversalDirection;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.core.dag.TraversalDirection;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Saga {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final EventStore eventStore;

  private final CompletionService<Operation> executorService = new ExecutorCompletionService<>(
      Executors.newFixedThreadPool(5));

  private final Set<Operation> completedTransactions;
  private final Set<Operation> completedCompensations;
  private final Set<SagaRequest> abortedTransactions;
  private final Set<SagaRequest> hangingOperations;

  private final TaskRunner transactionTaskRunner;
  private final TaskRunner compensationTaskRunner;
  private volatile SagaState currentTaskRunner;


  public Saga(EventStore eventStore, SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph) {
    this(eventStore, new BackwardRecovery(), sagaTaskGraph);
  }

  public Saga(EventStore eventStore, RecoveryPolicy recoveryPolicy,
      SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph) {

    this.eventStore = eventStore;
    this.completedTransactions = new HashSet<>();
    this.completedCompensations = new HashSet<>();
    this.abortedTransactions = new HashSet<>();
    this.hangingOperations = new HashSet<>();

    this.transactionTaskRunner = new TaskRunner(
        traveller(sagaTaskGraph, new FromRootTraversalDirection<>()),
        new TransactionTaskConsumer(executorService, new LoggingRecoveryPolicy(recoveryPolicy)));

    this.compensationTaskRunner = new TaskRunner(
        traveller(sagaTaskGraph, new FromLeafTraversalDirection<>()),
        new CompensationTaskConsumer(completedTransactions));

    currentTaskRunner = transactionTaskRunner;
  }

  public void run() {
    log.info("Starting Saga");
    do {
      try {
        currentTaskRunner.run();
      } catch (TransactionFailedException e) {
        log.error("Failed to run operation", e);
        currentTaskRunner = compensationTaskRunner;

        // is it possible that other parallel transactions haven't write start event to saga log by now?
        // if so, not all events are gathered here and some transactions will be missed
        gatherEvents(eventStore);

        hangingOperations.forEach(sagaTask -> {
          sagaTask.commit();
          sagaTask.compensate();
        });
      }
    } while (currentTaskRunner.hasNext());
    log.info("Completed Saga");
  }

  public void play() {
    log.info("Start playing events");
    gatherEvents(eventStore);

    transactionTaskRunner.replay(completedTransactions);

    if (!abortedTransactions.isEmpty() || !completedCompensations.isEmpty()) {
      currentTaskRunner = compensationTaskRunner;
      compensationTaskRunner.replay(completedCompensations);
    }

    log.info("Completed playing events");
  }

  private void gatherEvents(Iterable<SagaEvent> events) {
    for (SagaEvent event : events) {
      event.gatherTo(hangingOperations, abortedTransactions, completedTransactions, completedCompensations);
    }
  }

  private ByLevelTraveller<SagaRequest> traveller(
      SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph,
      TraversalDirection<SagaRequest> traversalDirection) {

    return new ByLevelTraveller<>(sagaTaskGraph, traversalDirection);
  }
}
