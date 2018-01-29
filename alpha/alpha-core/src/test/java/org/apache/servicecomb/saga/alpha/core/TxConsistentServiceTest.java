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

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.DONE;
import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.SagaStartedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.apache.servicecomb.saga.common.EventType;
import org.junit.Test;

public class TxConsistentServiceTest {
  private final Deque<TxEvent> events = new ConcurrentLinkedDeque<>();
  private final TxEventRepository eventRepository = new TxEventRepository() {
    @Override
    public void save(TxEvent event) {
      events.add(event);
    }

    @Override
    public Optional<TxEvent> findFirstAbortedGlobalTransaction() {
      return Optional.empty();
    }

    @Override
    public List<TxEvent> findTransactions(String globalTxId, String type) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId()) && type.equals(event.type()))
          .collect(Collectors.toList());
    }

    @Override
    public List<TxEvent> findFirstUncompensatedEventByIdGreaterThan(long id, String type) {
      return emptyList();
    }

    @Override
    public Optional<TxEvent> findFirstCompensatedEventByIdGreaterThan(long id, String type) {
      return Optional.empty();
    }

    @Override
    public void deleteDuplicateEvents(String type) {
    }
  };

  private final Deque<TxTimeout> timeouts = new ConcurrentLinkedDeque<>();
  private final TxTimeoutRepository timeoutRepository = new TxTimeoutRepository() {
    @Override
    public void save(TxTimeout timeout) {
      timeouts.add(timeout);
    }

    @Override
    public void markTxTimeoutAsDone(String globalTxId, String localTxId) {
      for (TxTimeout timeout : timeouts) {
        if (timeout.globalTxId().equals(globalTxId) &&
            timeout.localTxId().equals(localTxId)) {
          timeout.setStatus(DONE.name());
          break;
        }
      }
    }

    @Override
    public List<TxEvent> findFirstTimeoutTxToAbort() {
      return null;
    }
  };

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final String compensationMethod = getClass().getCanonicalName();

  private final TxConsistentService consistentService = new TxConsistentService(eventRepository, timeoutRepository);
  private final byte[] payloads = "yeah".getBytes();

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
    assertThat(timeouts.isEmpty(), is(true));
  }

  @Test
  public void skipTxStartedEvent_IfGlobalTxAlreadyFailed() {
    String localTxId1 = UUID.randomUUID().toString();
    events.add(newEvent(TxStartedEvent));
    events.add(newEvent(TxAbortedEvent));

    TxEvent event = eventOf(TxStartedEvent, localTxId1);

    consistentService.handle(event);

    assertThat(events.size(), is(2));
    assertThat(timeouts.isEmpty(), is(true));
  }

  @Test
  public void persistTimeoutEventOnArrival() {
    TxEvent[] events = {
        newEventWithTimeout(SagaStartedEvent, globalTxId,3),
        newEventWithTimeout(TxStartedEvent, 2),
        newEvent(TxEndedEvent),
        newEvent(TxCompensatedEvent),
        eventOf(SagaEndedEvent, globalTxId)};

    for (TxEvent event : events) {
      consistentService.handle(event);
    }

    assertThat(this.events, contains(events));
    assertThat(timeouts.size(), is(2));
    await().atMost(1, SECONDS).until(this::allTimeoutIsDone);
  }

  private boolean allTimeoutIsDone() {
    for (TxTimeout timeout : timeouts) {
      if (!timeout.status().equals(DONE.name())) {
        return false;
      }
    }
    return true;
  }

  private TxEvent newEvent(EventType eventType) {
    return newEventWithTimeout(eventType, 0);
  }

  private TxEvent newEventWithTimeout(EventType eventType, int timeout) {
    return newEventWithTimeout(eventType, localTxId, timeout);
  }

  private TxEvent newEventWithTimeout(EventType eventType, String localTxId, int timeout) {
    return new TxEvent(serviceName, instanceId, globalTxId, localTxId, parentTxId, eventType.name(), compensationMethod, timeout, payloads);
  }

  private TxEvent eventOf(EventType eventType, String localTxId) {
    return new TxEvent(serviceName,
        instanceId,
        globalTxId,
        localTxId,
        UUID.randomUUID().toString(),
        eventType.name(),
        compensationMethod,
        payloads);
  }
}
