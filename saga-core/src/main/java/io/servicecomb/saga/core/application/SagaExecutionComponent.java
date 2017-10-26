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

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.FallbackPolicy;
import io.servicecomb.saga.core.LoggingRecoveryPolicy;
import io.servicecomb.saga.core.PersistentLog;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.RecoveryPolicy;
import io.servicecomb.saga.core.RequestProcessTask;
import io.servicecomb.saga.core.Saga;
import io.servicecomb.saga.core.SagaContext;
import io.servicecomb.saga.core.SagaContextImpl;
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
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class SagaExecutionComponent {

  private final PersistentStore persistentStore;
  private final FromJsonFormat<Set<String>> childrenExtractor;
  private final FromJsonFormat<SagaDefinition> fromJsonFormat;
  private final ToJsonFormat toJsonFormat;
  private final Executor executorService;
  private final FallbackPolicy fallbackPolicy;
  private final GraphBuilder graphBuilder;
  private final RetrySagaLog retrySagaLog;

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
    this.fallbackPolicy = new FallbackPolicy(retryDelay);
    this.persistentStore = persistentStore;
    this.childrenExtractor = childrenExtractor;
    this.graphBuilder = new GraphBuilder(new GraphCycleDetectorImpl<>());
    this.fromJsonFormat = fromJsonFormat;
    this.toJsonFormat = toJsonFormat;
    this.executorService = executorService;
    this.retrySagaLog = new RetrySagaLog(persistentStore, retryDelay);
  }

  @Segment(name = "runSagaExecutionComponent", category = "application", library = "kamon")
  public String run(String requestJson) {
    String sagaId = UUID.randomUUID().toString();
    SagaContext sagaContext = new SagaContextImpl(childrenExtractor);
    EventStore sagaLog = new EmbeddedEventStore(sagaContext);
    SagaDefinition definition = fromJsonFormat.fromJson(requestJson);
    Saga saga = new Saga(
        sagaLog,
        executorService,
        sagaTasks(sagaId, requestJson, sagaLog, definition.policy()),
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
          sagaTasks(event.sagaId, requestJson, eventStore, definition.policy()),
          sagaContext,
          graphBuilder.build(definition.requests()));

      saga.play();
      saga.run();
    }
  }

  private CompositeSagaLog compositeSagaLog(SagaLog sagaLog, PersistentLog persistentLog) {
    return new CompositeSagaLog(sagaLog, persistentLog);
  }

  private Map<String, SagaTask> sagaTasks(String sagaId, String requestJson, EventStore sagaLog, RecoveryPolicy recoveryPolicy) {
    SagaLog compositeSagaLog = compositeSagaLog(sagaLog, persistentStore);

    return new HashMap<String, SagaTask>() {{
      put(SAGA_START_TASK, new SagaStartTask(sagaId, requestJson, compositeSagaLog));
      SagaLog retrySagaLog = compositeSagaLog(sagaLog, SagaExecutionComponent.this.retrySagaLog);
      put(SAGA_REQUEST_TASK, new RequestProcessTask(sagaId, retrySagaLog, new LoggingRecoveryPolicy(recoveryPolicy), fallbackPolicy));
      put(SAGA_END_TASK, new SagaEndTask(sagaId, retrySagaLog));
    }};
  }

  static class RetrySagaLog implements PersistentLog {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PersistentStore persistentStore;
    private final int retryDelay;

    RetrySagaLog(PersistentStore persistentStore, int retryDelay) {
      this.persistentStore = persistentStore;
      this.retryDelay = retryDelay;
    }

    @Override
    public void offer(SagaEvent sagaEvent) {
      boolean success = false;
      do {
        try {
          persistentStore.offer(sagaEvent);
          success = true;
          log.info("Persisted saga event {} successfully", sagaEvent);
        } catch (Exception e) {
          log.error("Failed to persist saga event {}", sagaEvent, e);
          sleep(retryDelay);
        }
      } while (!success && !isInterrupted());
    }

    private boolean isInterrupted() {
      return Thread.currentThread().isInterrupted();
    }

    private void sleep(int delay) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

  }
}
