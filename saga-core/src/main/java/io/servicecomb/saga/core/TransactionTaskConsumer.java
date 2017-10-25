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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;
import kamon.annotation.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EnableKamon
class TransactionTaskConsumer implements TaskConsumer {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<String, SagaTask> tasks;
  private final SagaContext sagaContext;
  private final CompletionService<Operation> executorService;

  TransactionTaskConsumer(
      Map<String, SagaTask> tasks,
      SagaContext sagaContext,
      CompletionService<Operation> executorService) {

    this.tasks = tasks;
    this.sagaContext = sagaContext;
    this.executorService = executorService;
  }

  @Segment(name = "consumeTask", category = "application", library = "kamon")
  @Override
  public void consume(Collection<Node<SagaRequest>> nodes) {
    List<Future<Operation>> futures = new ArrayList<>(nodes.size());
    for (Node<SagaRequest> node : nodes) {
      SagaRequest request = node.value();
      if (sagaContext.isChosenChild(request)) {
        futures.add(futureOf(request));
      }
    }

    for (int i = 0; i < futures.size(); i++) {
      try {
        executorService.take().get();
      } catch (ExecutionException e) {
        if (e.getCause() instanceof SagaStartFailedException) {
          throw ((SagaStartFailedException) e.getCause());
        }
        throw new TransactionFailedException(e.getCause());
      } catch (InterruptedException e) {
        // TODO: 7/29/2017 what shall we do when system is shutting down?
        throw new TransactionFailedException(e);
      }
    }
  }

  @Override
  public boolean replay(Collection<Node<SagaRequest>> nodes) {
    for (Iterator<Node<SagaRequest>> iterator = nodes.iterator(); iterator.hasNext(); ) {
      SagaRequest request = iterator.next().value();
      if (sagaContext.isTransactionCompleted(request)) {
        log.info("Skipped completed transaction id={} operation={} while replay", request.id(), request.transaction());
        iterator.remove();
      }
    }
    return !nodes.isEmpty();
  }

  @Segment(name = "submitCallable", category = "application", library = "kamon")
  private Future<Operation> futureOf(SagaRequest request) {
    return executorService.submit(new OperationCallable(tasks, request, sagaContext.responseOf(request.parents())));
  }

  @EnableKamon
  private static class OperationCallable implements Callable<Operation> {

    private final SagaRequest request;
    private final Map<String, SagaTask> tasks;
    private final SagaResponse parentResponse;

    private OperationCallable(
        Map<String, SagaTask> tasks,
        SagaRequest request,
        SagaResponse parentResponse) {
      this.request = request;
      this.tasks = tasks;
      this.parentResponse = parentResponse;
    }

    @Trace("runTransactionCallable")
    @Override
    public Operation call() throws Exception {
      tasks.get(request.task()).commit(request, parentResponse);
      return request.transaction();
    }
  }
}
