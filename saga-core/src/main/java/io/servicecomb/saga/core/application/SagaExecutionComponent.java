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

import static io.servicecomb.saga.core.SagaTask.SAGA_END_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_REQUEST_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_START_TASK;
import static java.lang.Thread.sleep;

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.FallbackPolicy;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.RequestProcessTask;
import io.servicecomb.saga.core.Saga;
import io.servicecomb.saga.core.SagaDefinition;
import io.servicecomb.saga.core.SagaEndTask;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaLog;
import io.servicecomb.saga.core.SagaStartTask;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.ToJsonFormat;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import io.servicecomb.saga.core.dag.GraphCycleDetectorImpl;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class SagaExecutionComponent {

  private final PersistentStore persistentStore;
  private final FromJsonFormat fromJsonFormat;
  private final ToJsonFormat toJsonFormat;
  private final Executor executorService;
  private final FallbackPolicy fallbackPolicy;
  private final GraphBuilder graphBuilder;
  private final RetrySagaLog retrySagaLog;

  public SagaExecutionComponent(
      PersistentStore persistentStore,
      FromJsonFormat fromJsonFormat,
      ToJsonFormat toJsonFormat) {
    this(
        500,
        persistentStore,
        fromJsonFormat,
        toJsonFormat,
        Executors.newFixedThreadPool(5));
  }

  public SagaExecutionComponent(
      int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat fromJsonFormat,
      ToJsonFormat toJsonFormat,
      ExecutorService executorService) {
    this.fallbackPolicy = new FallbackPolicy(retryDelay);
    this.persistentStore = persistentStore;
    this.graphBuilder = new GraphBuilder(new GraphCycleDetectorImpl<>());
    this.fromJsonFormat = fromJsonFormat;
    this.toJsonFormat = toJsonFormat;
    this.executorService = executorService;
    this.retrySagaLog = new RetrySagaLog(persistentStore, retryDelay);
  }

  @Segment(name = "runSagaExecutionComponent", category = "application", library = "kamon")
  public void run(String requestJson) {
    String sagaId = UUID.randomUUID().toString();
    EventStore sagaLog = new EmbeddedEventStore();

    SagaDefinition definition = fromJsonFormat.fromJson(requestJson);

    Saga saga = new Saga(
        sagaLog,
        executorService,
        definition.policy(),
        sagaTasks(sagaId, requestJson, sagaLog),
        graphBuilder.build(definition.requests()));
    saga.run();
  }

  public void reanimate() {
    Map<String, List<EventEnvelope>> pendingSagaEvents = persistentStore.findPendingSagaEvents();

    for (Entry<String, List<EventEnvelope>> entry : pendingSagaEvents.entrySet()) {
      EventStore eventStore = new EmbeddedEventStore();
      eventStore.populate(entry.getValue());
      SagaEvent event = entry.getValue().iterator().next().event;

      String requestJson = event.json(toJsonFormat);
      SagaDefinition definition = fromJsonFormat.fromJson(requestJson);

      Saga saga = new Saga(
          eventStore,
          executorService,
          definition.policy(),
          sagaTasks(event.sagaId, requestJson, eventStore),
          graphBuilder.build(definition.requests()));

      saga.play();
      saga.run();
    }
  }

  private CompositeSagaLog compositeSagaLog(SagaLog sagaLog, PersistentStore persistentStore) {
    return new CompositeSagaLog(sagaLog, persistentStore);
  }

  private Map<String, SagaTask> sagaTasks(String sagaId, String requestJson, EventStore sagaLog) {
    SagaLog compositeSagaLog = compositeSagaLog(sagaLog, persistentStore);

    return new HashMap<String, SagaTask>() {{
      put(SAGA_START_TASK, new SagaStartTask(sagaId, requestJson, compositeSagaLog));
      SagaLog retrySagaLog = compositeSagaLog(sagaLog, SagaExecutionComponent.this.retrySagaLog);
      put(SAGA_REQUEST_TASK, new RequestProcessTask(sagaId, retrySagaLog, fallbackPolicy));
      put(SAGA_END_TASK, new SagaEndTask(sagaId, retrySagaLog));
    }};
  }

  static class RetrySagaLog implements PersistentStore {

    private final PersistentStore retryPersistentStore;
    private final int retryDelay;

    RetrySagaLog(PersistentStore persistentStore, int retryDelay) {
      this.retryPersistentStore = persistentStore;
      this.retryDelay = retryDelay;
    }

    @Override
    public void offer(SagaEvent sagaEvent) {
      boolean success = false;
      do {
        try {
          retryPersistentStore.offer(sagaEvent);
          success = true;
        } catch (Exception e) {
          e.printStackTrace();
          try {
            sleep(retryDelay);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        }
      } while (!success);
    }

    @Override
    public long size() {
      return retryPersistentStore.size();
    }

    @Override
    public Map<String, List<EventEnvelope>> findPendingSagaEvents() {
      return retryPersistentStore.findPendingSagaEvents();
    }
  }
}
