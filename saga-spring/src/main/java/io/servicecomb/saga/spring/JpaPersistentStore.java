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
import java.util.Collections;
import java.util.Map;

class JpaPersistentStore implements PersistentStore {

  private final SagaEventRepo repo;

  JpaPersistentStore(SagaEventRepo repo) {
    this.repo = repo;
  }

  @Override
  public Map<String, Iterable<EventEnvelope>> findPendingSagaEvents() {
    return Collections.emptyMap();
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
