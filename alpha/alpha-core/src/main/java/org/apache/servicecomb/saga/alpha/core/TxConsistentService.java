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

import static java.util.Collections.emptySet;
import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TxConsistentService {
  private static final Consumer<TxEvent> DO_NOTHING_CONSUMER = event -> {};

  private static final byte[] EMPTY_PAYLOAD = new byte[0];

  private final TxEventRepository eventRepository;
  private final OmegaCallback omegaCallback;
  private final Map<String, Consumer<TxEvent>> eventCallbacks = new HashMap<String, Consumer<TxEvent>>() {{
    put(TxEndedEvent.name(), (event) -> compensateIfAlreadyAborted(event));
    put(TxAbortedEvent.name(), (event) -> compensate(event));
    put(TxCompensatedEvent.name(), (event) -> updateCompensateStatus(event));
  }};

  private final Map<String, Set<String>> eventsToCompensate = new HashMap<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public TxConsistentService(TxEventRepository eventRepository, OmegaCallback omegaCallback) {
    this.eventRepository = eventRepository;
    this.omegaCallback = omegaCallback;
  }

  public boolean handle(TxEvent event) {
    if (TxStartedEvent.name().equals(event.type()) && isGlobalTxAborted(event)) {
      return false;
    }

    eventRepository.save(event);

    executor.execute(() -> eventCallbacks.getOrDefault(event.type(), DO_NOTHING_CONSUMER).accept(event));
  }

  private void compensateIfAlreadyAborted(TxEvent event) {
    if (!isCompensationScheduled(event) && isGlobalTxAborted(event)) {
      eventsToCompensate.computeIfAbsent(event.globalTxId(), k -> new HashSet<>()).add(event.localTxId());
      TxEvent correspondingStartedEvent = eventRepository
          .findFirstTransaction(event.globalTxId(), event.localTxId(), TxStartedEvent.name());

      omegaCallback.compensate(correspondingStartedEvent);
    }
  }

  private boolean isCompensationScheduled(TxEvent event) {
    return eventsToCompensate.getOrDefault(event.globalTxId(), emptySet()).contains(event.localTxId());

    return true;
  }

  private void compensate(TxEvent event) {
    List<TxEvent> events = eventRepository.findTransactionsToCompensate(event.globalTxId());

    events.removeIf(this::isCompensationScheduled);

    Set<String> localTxIds = eventsToCompensate.computeIfAbsent(event.globalTxId(), k -> new HashSet<>());
    events.forEach(e -> localTxIds.add(e.localTxId()));

    events.forEach(omegaCallback::compensate);
  }

  // TODO: 2018/1/13 SagaEndedEvent may still not be the last, because some omegas may have slow network and its TxEndedEvent reached late,
  // unless we ask user to specify a name for each participant in the global TX in @Compensable
  private void updateCompensateStatus(TxEvent event) {
    Set<String> events = eventsToCompensate.get(event.globalTxId());
    if (events != null) {
      events.remove(event.localTxId());
      if (events.isEmpty()) {
        markGlobalTxEnd(event);
        eventsToCompensate.remove(event.globalTxId());
      }
    }
  }

  private void markGlobalTxEnd(TxEvent event) {
    eventRepository.save(new TxEvent(
        event.serviceName(), event.instanceId(), new Date(), event.globalTxId(), event.globalTxId(),
        null, SagaEndedEvent.name(), "", EMPTY_PAYLOAD));
  }

  private boolean isGlobalTxAborted(TxEvent event) {
    return !eventRepository.findTransactions(event.globalTxId(), TxAbortedEvent.name()).isEmpty();
  }
}
