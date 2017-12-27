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

import static io.servicecomb.saga.alpha.core.EventType.SagaEndedEvent;
import static io.servicecomb.saga.alpha.core.EventType.SagaStartedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxAbortedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxCompensatedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxEndedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxStartedEvent;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;

public class TxConsistentServiceTest {
  private final List<TxEvent> events = new ArrayList<>();
  private final TxEventRepository eventRepository = new TxEventRepository() {
    @Override
    public void save(TxEvent event) {
      events.add(event);
    }

    @Override
    public List<TxEvent> findCompletedEvents(String globalTxId, String type) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId()) && type.equals(event.type()))
          .collect(Collectors.toList());
    }
  };

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();

  private final Map<String, List<byte[]>> callbackArgs = new HashMap<>();
  private final OmegaCallback omegaCallback = (key, value) -> callbackArgs.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  private final TxConsistentService consistentService = new TxConsistentService(eventRepository, omegaCallback);

  @Test
  public void persistEventOnArrival() throws Exception {
    TxEvent[] events = {
        newEvent(SagaStartedEvent),
        newEvent(TxStartedEvent),
        newEvent(TxEndedEvent),
        newEvent(TxCompensatedEvent),
        newEvent(SagaEndedEvent)};

    for (TxEvent event : events) {
      consistentService.handle(event);
    }

    assertThat(this.events, contains(events));
    assertThat(callbackArgs.isEmpty(), is(true));
  }

  @Test
  public void compensateGlobalTx_OnAnyLocalTxFailure() throws Exception {
    events.add(eventOf(TxStartedEvent, "service a".getBytes()));
    events.add(eventOf(TxEndedEvent, "service a".getBytes()));
    events.add(eventOf(TxStartedEvent, "service b".getBytes()));
    events.add(eventOf(TxEndedEvent, "service b".getBytes()));

    TxEvent abortEvent = newEvent(TxAbortedEvent);

    consistentService.handle(abortEvent);

    await().atMost(1, SECONDS).until(() -> callbackArgs.getOrDefault(globalTxId, emptyList()).size() > 1);
    assertThat(stringOf(callbackArgs.get(globalTxId)), containsInAnyOrder("service a", "service b"));
  }

  private List<String> stringOf(List<byte[]> bytes) {
    return bytes.stream()
        .map(String::new)
        .collect(Collectors.toList());
  }

  private TxEvent newEvent(EventType eventType) {
    return new TxEvent(new Date(), globalTxId, localTxId, parentTxId, eventType.name(), "yeah".getBytes());
  }

  private TxEvent eventOf(EventType eventType, byte[] payloads) {
    return new TxEvent(new Date(),
        globalTxId,
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        eventType.name(),
        payloads);
  }
}
