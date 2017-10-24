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

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.servicecomb.saga.core.dag.ByLevelTraveller;
import io.servicecomb.saga.core.dag.FromLeafTraversalDirection;
import io.servicecomb.saga.core.dag.FromRootTraversalDirection;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.core.dag.TraversalDirection;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class Saga {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final EventStore eventStore;
  private final Map<String, SagaTask> tasks;

  private final TaskRunner transactionTaskRunner;
  private final TaskRunner compensationTaskRunner;
  private final SagaContext sagaContext;
  private volatile SagaState currentTaskRunner;


  Saga(
      EventStore eventStore,
      Map<String, SagaTask> tasks,
      SagaContext sagaContext,
      SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph) {
    this(eventStore, new BackwardRecovery(), tasks, sagaContext, sagaTaskGraph);
  }

  Saga(EventStore eventStore,
      RecoveryPolicy recoveryPolicy,
      Map<String, SagaTask> tasks,
      SagaContext sagaContext,
      SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph) {

    this(eventStore, Executors.newFixedThreadPool(5), recoveryPolicy, tasks, sagaContext, sagaTaskGraph);
  }

  public Saga(EventStore eventStore,
      Executor executor,
      RecoveryPolicy recoveryPolicy,
      Map<String, SagaTask> tasks,
      SagaContext sagaContext,
      SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph) {

    this.eventStore = eventStore;
    this.tasks = tasks;

    this.transactionTaskRunner = new TaskRunner(
        traveller(sagaTaskGraph, new FromRootTraversalDirection<>()),
        new TransactionTaskConsumer(
            tasks,
            sagaContext,
            new ExecutorCompletionService<>(executor),
            new LoggingRecoveryPolicy(recoveryPolicy)));

    this.sagaContext = sagaContext;
    this.compensationTaskRunner = new TaskRunner(
        traveller(sagaTaskGraph, new FromLeafTraversalDirection<>()),
        new CompensationTaskConsumer(tasks, sagaContext));

    currentTaskRunner = transactionTaskRunner;
  }

  @Segment(name = "runSaga", category = "application", library = "kamon")
  public String run() {
    String failureInfo = null;
    log.info("Starting Saga");
    do {
      try {
        currentTaskRunner.run();
      } catch (TransactionFailedException e) {
        failureInfo = e.getMessage();
        log.error("Failed to run operation", e);
        currentTaskRunner = compensationTaskRunner;

        sagaContext.handleHangingTransactions(request -> {
          tasks.get(request.task()).commit(request, sagaContext.responseOf(request.parents()));
          tasks.get(request.task()).compensate(request);
        });
      }
    } while (currentTaskRunner.hasNext());
    log.info("Completed Saga");
    return failureInfo;
  }

  public void play() {
    log.info("Start playing events");
    gatherEvents(eventStore);

    transactionTaskRunner.replay();

    if (sagaContext.isCompensationStarted()) {
      currentTaskRunner = compensationTaskRunner;
      compensationTaskRunner.replay();
    }

    log.info("Completed playing events");
  }

  private void gatherEvents(Iterable<SagaEvent> events) {
    for (SagaEvent event : events) {
      event.gatherTo(sagaContext);
    }
  }

  private ByLevelTraveller<SagaRequest> traveller(
      SingleLeafDirectedAcyclicGraph<SagaRequest> sagaTaskGraph,
      TraversalDirection<SagaRequest> traversalDirection) {

    return new ByLevelTraveller<>(sagaTaskGraph, traversalDirection);
  }
}
