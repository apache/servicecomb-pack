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

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.servicecomb.saga.core.EventEnvelope;
import org.apache.servicecomb.saga.core.EventStore;
import org.apache.servicecomb.saga.core.SagaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedEventStore implements EventStore {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Queue<SagaEvent> events = new LinkedBlockingQueue<>();

  @Override
  public void offer(SagaEvent sagaEvent) {
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
  public Iterator<SagaEvent> iterator() {
    return events.iterator();
  }
}
