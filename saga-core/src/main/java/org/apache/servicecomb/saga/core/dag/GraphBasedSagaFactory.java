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

package org.apache.servicecomb.saga.core.dag;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.servicecomb.saga.core.EventStore;
import org.apache.servicecomb.saga.core.GraphBasedSaga;
import org.apache.servicecomb.saga.core.Saga;
import org.apache.servicecomb.saga.core.SagaContext;
import org.apache.servicecomb.saga.core.SagaContextImpl;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.application.SagaFactory;
import org.apache.servicecomb.saga.infrastructure.ContextAwareEventStore;
import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaTaskFactory;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;

public class GraphBasedSagaFactory implements SagaFactory {
  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final FromJsonFormat<Set<String>> childrenExtractor;
  private final Executor executorService;
  private final GraphBuilder graphBuilder;
  private final SagaTaskFactory sagaTaskFactory;

  public GraphBasedSagaFactory(int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat<Set<String>> childrenExtractor,
      ExecutorService executorService) {

    this.childrenExtractor = childrenExtractor;
    this.executorService = executorService;
    this.sagaTaskFactory = new SagaTaskFactory(retryDelay, persistentStore);
    this.graphBuilder = new GraphBuilder(new GraphCycleDetectorImpl<SagaRequest>());
  }

  @Override
  public Saga createSaga(String requestJson, String sagaId, EventStore sagaLog, SagaDefinition definition) {
    SagaContext sagaContext = new SagaContextImpl(childrenExtractor);

    return new GraphBasedSaga(
        sagaLog,
        executorService,
        sagaTaskFactory.sagaTasks(sagaId,
            requestJson,
            definition.policy(),
            new ContextAwareEventStore(sagaLog, sagaContext)
        ),
        sagaContext,
        graphBuilder.build(definition.requests()));
  }

  @Override
  public boolean isTerminated() {
    return !isRunning.get();
  }

  @Override
  public void terminate() throws Exception {
    isRunning.compareAndSet(true, false);
  }
}
