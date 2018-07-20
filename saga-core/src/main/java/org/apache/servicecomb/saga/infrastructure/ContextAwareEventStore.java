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

package org.apache.servicecomb.saga.infrastructure;

import java.util.Iterator;

import org.apache.servicecomb.saga.core.EventEnvelope;
import org.apache.servicecomb.saga.core.EventStore;
import org.apache.servicecomb.saga.core.SagaContext;
import org.apache.servicecomb.saga.core.SagaEvent;

public class ContextAwareEventStore implements EventStore {
  private final EventStore eventStore;
  private final SagaContext sagaContext;

  public ContextAwareEventStore(EventStore eventStore, SagaContext sagaContext) {
    this.eventStore = eventStore;
    this.sagaContext = sagaContext;
  }

  @Override
  public void offer(SagaEvent sagaEvent) {
    sagaEvent.gatherTo(sagaContext);
    eventStore.offer(sagaEvent);
  }

  @Override
  public void populate(Iterable<EventEnvelope> events) {
    eventStore.populate(events);
  }

  @Override
  public long size() {
    return eventStore.size();
  }

  @Override
  public Iterator<SagaEvent> iterator() {
    return eventStore.iterator();
  }
}
