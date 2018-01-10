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

package org.apache.servicecomb.saga.alpha.core;

import static org.apache.servicecomb.saga.alpha.core.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxCompensatedEvent;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxStartedEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class TxConsistentService {
  private static final Consumer<TxEvent> DO_NOTHING_CONSUMER = event -> {};

  private static final byte[] EMPTY_PAYLOAD = new byte[0];

  private final TxEventRepository eventRepository;
  private final OmegaCallback omegaCallback;
  private final Map<String, Consumer<TxEvent>> eventCallbacks = new HashMap<String, Consumer<TxEvent>>() {{
    put(TxStartedEvent.name(), DO_NOTHING_CONSUMER);
    put(TxAbortedEvent.name(), (event) -> compensate(event));
    put(TxCompensatedEvent.name(), (event) -> updateCompensateStatus(event));
  }};

  private final Map<String, Set<String>> eventsToCompensate = new ConcurrentHashMap<>();

  public TxConsistentService(TxEventRepository eventRepository, OmegaCallback omegaCallback) {
    this.eventRepository = eventRepository;
    this.omegaCallback = omegaCallback;
  }

  public void handle(TxEvent event) {
    eventRepository.save(event);
    CompletableFuture.runAsync(() -> eventCallbacks.getOrDefault(event.type(), DO_NOTHING_CONSUMER).accept(event));
  }

  private void compensate(TxEvent event) {
    List<TxEvent> events = eventRepository.findStartedTransactions(event.globalTxId(), TxStartedEvent.name());
    events.forEach(omegaCallback::compensate);
    eventsToCompensate.computeIfAbsent(event.globalTxId(), (v) -> {
      CopyOnWriteArraySet<String> eventSet = new CopyOnWriteArraySet<>();
      events.forEach(e -> eventSet.add(getUniqueEventId(e)));
      return eventSet;
    });
  }

  private void updateCompensateStatus(TxEvent event) {
    Set<String> events = eventsToCompensate.get(event.globalTxId());
    if (events != null) {
      events.remove(getUniqueEventId(event));
      if (events.isEmpty()) {
        markGlobalTxEnd(event);
      }
    }
  }

  private String getUniqueEventId(TxEvent event) {
    return event.globalTxId() + "_" + event.localTxId();
  }

  private void markGlobalTxEnd(TxEvent event) {
    eventRepository.save(new TxEvent(
        event.serviceName(), event.instanceId(), new Date(), event.globalTxId(), event.globalTxId(),
        null, SagaEndedEvent.name(), "", EMPTY_PAYLOAD));
  }
}
