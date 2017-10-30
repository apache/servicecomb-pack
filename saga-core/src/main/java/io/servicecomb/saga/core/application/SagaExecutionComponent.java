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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.Saga;
import io.servicecomb.saga.core.SagaContext;
import io.servicecomb.saga.core.SagaContextImpl;
import io.servicecomb.saga.core.SagaDefinition;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.ToJsonFormat;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import io.servicecomb.saga.core.dag.GraphCycleDetectorImpl;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class SagaExecutionComponent {

  private final PersistentStore persistentStore;
  private final FromJsonFormat<Set<String>> childrenExtractor;
  private final FromJsonFormat<SagaDefinition> fromJsonFormat;
  private final ToJsonFormat toJsonFormat;
  private final Executor executorService;
  private final GraphBuilder graphBuilder;
  private final SagaTaskFactory sagaTaskFactory;

  public SagaExecutionComponent(
      PersistentStore persistentStore,
      FromJsonFormat<SagaDefinition> fromJsonFormat,
      ToJsonFormat toJsonFormat,
      FromJsonFormat<Set<String>> childrenExtractor) {
    this(
        500,
        persistentStore,
        fromJsonFormat,
        toJsonFormat,
        childrenExtractor,
        Executors.newFixedThreadPool(5));
  }

  public SagaExecutionComponent(
      int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat<SagaDefinition> fromJsonFormat,
      ToJsonFormat toJsonFormat,
      FromJsonFormat<Set<String>> childrenExtractor,
      ExecutorService executorService) {
    this.sagaTaskFactory = new SagaTaskFactory(retryDelay, persistentStore);
    this.persistentStore = persistentStore;
    this.childrenExtractor = childrenExtractor;
    this.graphBuilder = new GraphBuilder(new GraphCycleDetectorImpl<>());
    this.fromJsonFormat = fromJsonFormat;
    this.toJsonFormat = toJsonFormat;
    this.executorService = executorService;
  }

  @Segment(name = "runSagaExecutionComponent", category = "application", library = "kamon")
  public SagaResponse run(String requestJson) {
    String sagaId = UUID.randomUUID().toString();
    SagaContext sagaContext = new SagaContextImpl(childrenExtractor);
    EventStore sagaLog = new EmbeddedEventStore(sagaContext);
    SagaDefinition definition = fromJsonFormat.fromJson(requestJson);
    Saga saga = new Saga(
        sagaLog,
        executorService,
        sagaTaskFactory.sagaTasks(sagaId, requestJson, definition.policy(), sagaLog, persistentStore),
        sagaContext,
        graphBuilder.build(definition.requests()));
    return saga.run();
  }

  public void reanimate() {
    Map<String, List<EventEnvelope>> pendingSagaEvents = persistentStore.findPendingSagaEvents();

    for (Entry<String, List<EventEnvelope>> entry : pendingSagaEvents.entrySet()) {
      SagaContext sagaContext = new SagaContextImpl(childrenExtractor);
      EventStore eventStore = new EmbeddedEventStore(sagaContext);
      eventStore.populate(entry.getValue());
      SagaEvent event = entry.getValue().iterator().next().event;

      String requestJson = event.json(toJsonFormat);
      SagaDefinition definition = fromJsonFormat.fromJson(requestJson);

      Saga saga = new Saga(
          eventStore,
          executorService,
          sagaTaskFactory.sagaTasks(event.sagaId, requestJson, definition.policy(), eventStore, persistentStore),
          sagaContext,
          graphBuilder.build(definition.requests()));

      saga.play();
      saga.run();
    }
  }
}
