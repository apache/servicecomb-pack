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

package org.apache.servicecomb.saga.core;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.servicecomb.saga.core.dag.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompensationTaskConsumer implements TaskConsumer {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<String, SagaTask> tasks;
  private final SagaContext sagaContext;

  CompensationTaskConsumer(Map<String, SagaTask> tasks, SagaContext sagaContext) {
    this.tasks = tasks;
    this.sagaContext = sagaContext;
  }

  @Override
  public void consume(Collection<Node<SagaRequest>> nodes) {
    for (Node<SagaRequest> node : nodes) {
      SagaRequest request = node.value();

      if (sagaContext.isTransactionCompleted(request)) {
        log.info("Starting request {} id={}", request.serviceName(), request.id());
        tasks.get(request.task()).compensate(request);
        log.info("Completed request {} id={}", request.serviceName(), request.id());
      }
    }
  }

  @Override
  public boolean replay(Collection<Node<SagaRequest>> nodes) {

    for (Iterator<Node<SagaRequest>> iterator = nodes.iterator(); iterator.hasNext(); ) {
      SagaRequest request = iterator.next().value();
      if (sagaContext.isCompensationCompleted(request)) {
        log.info("Skipped completed compensation id={} operation={} while replay", request.id(), request.transaction());
        iterator.remove();
      } else if (!sagaContext.isTransactionCompleted(request)) {
        // this transaction never started
        log.info("Skipped pending transaction id={} operation={} while replay", request.id(), request.transaction());
        iterator.remove();
      }
    }
    return !nodes.isEmpty();
  }
}
