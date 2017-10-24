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

package io.servicecomb.saga.infrastructure;

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.SagaContext;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaResponse;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedEventStore implements EventStore {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Queue<SagaEvent> events = new LinkedBlockingQueue<>();
  private final SagaContext sagaContext;

  public EmbeddedEventStore(SagaContext sagaContext) {
    this.sagaContext = sagaContext;
  }

  @Override
  public void offer(SagaEvent sagaEvent) {
    sagaEvent.gatherTo(sagaContext);
    events.offer(sagaEvent);
    log.info("Added event {}", sagaEvent);
  }

  @Override
  public void populate(Iterable<EventEnvelope> events) {
    for (EventEnvelope event : events) {
      this.events.offer(event.event);
      log.info("Populated event {}", event);
    }
  }

  @Override
  public long size() {
    return events.size();
  }

  @Override
  public SagaResponse responseOf(String[] parentRequestIds) {
    return sagaContext.responseOf(parentRequestIds);
  }

  @Override
  public Iterator<SagaEvent> iterator() {
    return events.iterator();
  }
}
