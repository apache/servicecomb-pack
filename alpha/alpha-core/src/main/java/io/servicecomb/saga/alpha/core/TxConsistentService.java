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

package io.servicecomb.saga.alpha.core;

import static io.servicecomb.saga.alpha.core.EventType.TxAbortedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxStartedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TxConsistentService {
  private static final Consumer<TxEvent> DO_NOTHING_CONSUMER = event -> {};

  private final TxEventRepository eventRepository;
  private final OmegaCallback omegaCallback;
  private final Map<String, Consumer<TxEvent>> eventCallbacks = new HashMap<String, Consumer<TxEvent>>() {{
    put(TxStartedEvent.name(), DO_NOTHING_CONSUMER);
    put(TxAbortedEvent.name(), (event) -> compensate(event));
  }};

  public TxConsistentService(TxEventRepository eventRepository, OmegaCallback omegaCallback) {
    this.eventRepository = eventRepository;
    this.omegaCallback = omegaCallback;
  }

  public void handle(TxEvent event) {
    eventRepository.save(event);
    CompletableFuture.runAsync(() -> eventCallbacks.getOrDefault(event.type(), DO_NOTHING_CONSUMER).accept(event));
  }

  // TODO: 2017/12/27 we must define a way to find which service to compensate, to avoid sending to all
  private void compensate(TxEvent event) {
    List<TxEvent> events = eventRepository.findStartedTransactions(event.globalTxId(), TxStartedEvent.name());
    events.forEach(evt -> omegaCallback.compensate(evt.globalTxId(), evt.localTxId(), event.compensationMethod(), evt.payloads()));
  }
}
