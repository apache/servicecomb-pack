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

package org.apache.servicecomb.saga.spring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.servicecomb.saga.core.EventEnvelope;
import org.apache.servicecomb.saga.core.ToJsonFormat;
import org.apache.servicecomb.saga.format.SagaEventFormat;
import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.SagaEvent;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
class JpaPersistentStore implements PersistentStore {

  private final SagaEventRepo repo;
  private final SagaEventFormat sagaEventFormat;
  private final ToJsonFormat toJsonFormat;

  JpaPersistentStore(SagaEventRepo repo, ToJsonFormat toJsonFormat, SagaEventFormat sagaEventFormat) {
    this.repo = repo;
    this.sagaEventFormat = sagaEventFormat;
    this.toJsonFormat = toJsonFormat;
  }

  @Override
  public Map<String, List<EventEnvelope>> findPendingSagaEvents() {
    List<SagaEventEntity> events = repo.findIncompleteSagaEventsGroupBySagaId();

    Map<String, List<EventEnvelope>> pendingEvents = new HashMap<>();
    for (SagaEventEntity event : events) {
      pendingEvents.computeIfAbsent(event.sagaId(), id -> new LinkedList<>());
      pendingEvents.get(event.sagaId()).add(
          new EventEnvelope(
              event.id(),
              event.creationTime(),
              sagaEventFormat.toSagaEvent(event.sagaId(), event.type(), event.contentJson())));
    }

    return pendingEvents;
  }

  @Segment(name = "save", category = "database", library = "kamon")
  @Override
  public void offer(SagaEvent event) {
    repo.save(new SagaEventEntity(event.sagaId, event.getClass().getSimpleName(), event.json(toJsonFormat)));
  }
}
