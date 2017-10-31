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

package io.servicecomb.saga.core.application;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.GraphBasedSaga;
import io.servicecomb.saga.core.Saga;
import io.servicecomb.saga.core.SagaContext;
import io.servicecomb.saga.core.SagaContextImpl;
import io.servicecomb.saga.core.SagaDefinition;
import io.servicecomb.saga.core.SagaTaskFactory;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import io.servicecomb.saga.core.dag.GraphCycleDetectorImpl;
import io.servicecomb.saga.infrastructure.ContextAwareEventStore;

class GraphBasedSagaFactory implements SagaFactory {
  private final FromJsonFormat<Set<String>> childrenExtractor;
  private final Executor executorService;
  private final GraphBuilder graphBuilder;
  private final SagaTaskFactory sagaTaskFactory;
  private final PersistentStore persistentStore;

  GraphBasedSagaFactory(int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat<Set<String>> childrenExtractor,
      ExecutorService executorService) {

    this.childrenExtractor = childrenExtractor;
    this.executorService = executorService;
    this.persistentStore = persistentStore;
    this.sagaTaskFactory = new SagaTaskFactory(retryDelay, persistentStore);
    this.graphBuilder = new GraphBuilder(new GraphCycleDetectorImpl<>());
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
}