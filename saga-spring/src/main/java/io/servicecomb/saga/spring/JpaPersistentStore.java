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

package io.servicecomb.saga.spring;

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.SagaEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class JpaPersistentStore implements PersistentStore {

  private final SagaEventRepo repo;
  private final SagaEventFormat sagaEventFormat;

  JpaPersistentStore(SagaEventRepo repo) {
    this.repo = repo;
    this.sagaEventFormat = new SagaEventFormat();
  }

  @Override
  public Map<String, List<EventEnvelope>> findPendingSagaEvents() {
    List<SagaEventEntity> events = repo.findIncompleteSagaEventsGroupBySagaId();

    Map<String, List<EventEnvelope>> pendingEvents = new HashMap<>();
    for (SagaEventEntity event : events) {
      pendingEvents.computeIfAbsent(event.sagaId(), id -> new LinkedList<>());
      pendingEvents.get(event.sagaId()).add(
          new EventEnvelope(event.id(), event.creationTime(), sagaEventFormat.toSagaEvent(event)));
    }

    return pendingEvents;
  }

  @Override
  public void offer(SagaEvent event) {
    repo.save(new SagaEventEntity(event.sagaId, event.getClass().getSimpleName(), event.json()));
  }

  @Override
  public long size() {
    return repo.count();
  }
}
