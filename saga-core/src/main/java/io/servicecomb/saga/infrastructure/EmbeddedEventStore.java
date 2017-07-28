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

import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.SagaEvent;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedEventStore implements EventStore {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Set<SagaEvent> events = new ConcurrentSkipListSet<>((o1, o2) -> (int) (o1.id() - o2.id()));

  @Override
  public void offer(SagaEvent sagaEvent) {
    events.add(sagaEvent);
    log.info("Added event id={}, type={}", sagaEvent.id(), sagaEvent.description());
  }

  @Override
  public int size() {
    return events.size();
  }

  @Override
  public Iterator<SagaEvent> iterator() {
    return events.iterator();
  }
}
