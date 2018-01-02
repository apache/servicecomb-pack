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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
    public List<TxEvent> findStartedTransactions(String globalTxId, String type) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId()) && type.equals(event.type()))
          .collect(Collectors.toList());
    }
  };

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final String compensationMethod = getClass().getCanonicalName();
  private final List<CompensationContext> compensationContexts = new ArrayList<>();

  private final OmegaCallback omegaCallback = event ->
      compensationContexts.add(new CompensationContext(event.globalTxId(), event.localTxId(), event.compensationMethod(), event.payloads()));

  private final TxConsistentService consistentService = new TxConsistentService(eventRepository, omegaCallback);

  @Test
  public void persistEventOnArrival() throws Exception {
    TxEvent[] events = {
        newEvent(EventType.SagaStartedEvent),
        newEvent(EventType.TxStartedEvent),
        newEvent(EventType.TxEndedEvent),
        newEvent(EventType.TxCompensatedEvent),
        newEvent(EventType.SagaEndedEvent)};

    for (TxEvent event : events) {
      consistentService.handle(event);
    }

    assertThat(this.events, contains(events));
    assertThat(compensationContexts.isEmpty(), is(true));
  }

  @Test
  public void compensateGlobalTx_OnAnyLocalTxFailure() throws Exception {
    String localTxId1 = UUID.randomUUID().toString();
    events.add(eventOf(EventType.TxStartedEvent, "service a".getBytes(), localTxId1, "method a"));
    events.add(eventOf(EventType.TxEndedEvent, new byte[0], localTxId1, "method a"));

    String localTxId2 = UUID.randomUUID().toString();
    events.add(eventOf(EventType.TxStartedEvent, "service b".getBytes(), localTxId2, "method b"));
    events.add(eventOf(EventType.TxEndedEvent, new byte[0], localTxId2, "method b"));

    TxEvent abortEvent = newEvent(EventType.TxAbortedEvent);

    consistentService.handle(abortEvent);

    await().atMost(1, SECONDS).until(() -> compensationContexts.size() > 1);
    assertThat(compensationContexts, containsInAnyOrder(
        new CompensationContext(globalTxId, localTxId1, "method a", "service a".getBytes()),
        new CompensationContext(globalTxId, localTxId2, "method b", "service b".getBytes())
    ));
  }

  private TxEvent newEvent(EventType eventType) {
    return new TxEvent(serviceName, instanceId, new Date(), globalTxId, localTxId, parentTxId, eventType.name(), compensationMethod, "yeah".getBytes());
  }

  private TxEvent eventOf(EventType eventType, byte[] payloads, String localTxId, String compensationMethod) {
    return new TxEvent(serviceName, instanceId, new Date(),
        globalTxId,
        localTxId,
        UUID.randomUUID().toString(),
        eventType.name(),
        compensationMethod,
        payloads);
  }

  private static class CompensationContext {
    private final String globalTxId;
    private final String localTxId;
    private final String compensationMethod;
    private final byte[] message;

    private CompensationContext(String globalTxId, String localTxId, String compensationMethod, byte[] message) {
      this.globalTxId = globalTxId;
      this.localTxId = localTxId;
      this.compensationMethod = compensationMethod;
      this.message = message;
    }

    @Override
    public String toString() {
      return "CompensationContext{" +
          "globalTxId='" + globalTxId + '\'' +
          ", localTxId='" + localTxId + '\'' +
          ", compensationMethod='" + compensationMethod + '\'' +
          ", message=" + Arrays.toString(message) +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CompensationContext that = (CompensationContext) o;
      return Objects.equals(globalTxId, that.globalTxId) &&
          Objects.equals(localTxId, that.localTxId) &&
          Objects.equals(compensationMethod, that.compensationMethod) &&
          Arrays.equals(message, that.message);
    }

    @Override
    public int hashCode() {
      return Objects.hash(globalTxId, localTxId, compensationMethod, message);
    }
  }
}
