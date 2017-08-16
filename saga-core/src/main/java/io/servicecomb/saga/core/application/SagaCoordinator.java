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

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.Saga;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.application.interpreter.JsonRequestInterpreter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class SagaCoordinator {

  private final EventStore eventStore;
  private final PersistentStore persistentStore;
  private final JsonRequestInterpreter requestInterpreter;
  private final AtomicLong sagaIdGenerator = new AtomicLong();

  public SagaCoordinator(EventStore eventStore, PersistentStore persistentStore, JsonRequestInterpreter requestInterpreter) {
    this.eventStore = eventStore;
    this.persistentStore = persistentStore;
    this.requestInterpreter = requestInterpreter;
  }

  public void run(String requestJson) {
    // TODO: 8/11/2017 pass persistent store to saga too
    Saga saga = new Saga(eventStore, requestInterpreter.interpret(sagaIdGenerator.incrementAndGet(), requestJson));

    saga.run();
  }

  public void reanimate() {
    Map<Long, Iterable<EventEnvelope>> pendingSagaEvents = persistentStore.findPendingSagaEvents();
    for (Entry<Long, Iterable<EventEnvelope>> entry : pendingSagaEvents.entrySet()) {
      eventStore.populate(entry.getValue());
      SagaEvent event = eventStore.peek();

      Saga saga = new Saga(eventStore, requestInterpreter.interpret(event.sagaId, event.payload().json()));

      saga.play();
      saga.run();
    }
  }
}
