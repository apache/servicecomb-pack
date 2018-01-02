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

package org.apache.servicecomb.saga.core.application;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.servicecomb.saga.core.EventEnvelope;
import org.apache.servicecomb.saga.core.EventStore;
import org.apache.servicecomb.saga.core.Saga;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.ToJsonFormat;
import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaEvent;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import org.apache.servicecomb.saga.infrastructure.EmbeddedEventStore;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class SagaExecutionComponent {

  private final PersistentStore persistentStore;
  private final FromJsonFormat<SagaDefinition> fromJsonFormat;
  private final ToJsonFormat toJsonFormat;
  private final SagaFactory sagaFactory;

  public SagaExecutionComponent(
      PersistentStore persistentStore,
      FromJsonFormat<SagaDefinition> fromJsonFormat,
      ToJsonFormat toJsonFormat,
      SagaFactory sagaFactory) {
    this.persistentStore = persistentStore;
    this.fromJsonFormat = fromJsonFormat;
    this.toJsonFormat = toJsonFormat;
    this.sagaFactory = sagaFactory;
  }

  @Segment(name = "runSagaExecutionComponent", category = "application", library = "kamon")
  public SagaResponse run(String requestJson) {
    String sagaId = UUID.randomUUID().toString();
    EventStore sagaLog = new EmbeddedEventStore();
    SagaDefinition definition = fromJsonFormat.fromJson(requestJson);
    Saga saga = sagaFactory.createSaga(requestJson, sagaId, sagaLog, definition);
    return saga.run();
  }

  public void reanimate() {
    Map<String, List<EventEnvelope>> pendingSagaEvents = persistentStore.findPendingSagaEvents();

    for (Entry<String, List<EventEnvelope>> entry : pendingSagaEvents.entrySet()) {
      EventStore eventStore = new EmbeddedEventStore();
      eventStore.populate(entry.getValue());
      SagaEvent event = entry.getValue().iterator().next().event;

      String requestJson = event.json(toJsonFormat);
      SagaDefinition definition = fromJsonFormat.fromJson(requestJson);

      Saga saga = sagaFactory.createSaga(requestJson, event.sagaId, eventStore, definition);

      saga.play();
      saga.run();
    }
  }

  public void terminate() throws Exception {
    sagaFactory.terminate();
  }
}
