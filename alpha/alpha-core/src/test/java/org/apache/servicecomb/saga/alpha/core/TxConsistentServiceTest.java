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
import static org.apache.servicecomb.saga.alpha.core.CommandStatus.DONE;
import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.SagaStartedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
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
    public List<TxEvent> findTransactions(String globalTxId, String type) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId()) && type.equals(event.type()))
          .collect(Collectors.toList());
    }

    @Override
    public TxEvent findFirstTransaction(String globalTxId, String localTxId, String type) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId()) && localTxId.equals(event.localTxId()) && type.equals(event.type()))
          .findFirst()
          .get();
    }

    @Override
    public List<TxEvent> findTransactionsToCompensate(String globalTxId) {
      return events.stream()
          .filter(event -> globalTxId.equals(event.globalTxId())
              && event.type().equals(TxStartedEvent.name())
              && isCompleted(globalTxId, event)
              && !isCompensated(globalTxId, event))
          .collect(Collectors.toList());
    }

    private boolean isCompleted(String globalTxId, TxEvent event) {
      return events.stream()
          .filter(e -> globalTxId.equals(e.globalTxId())
              && e.localTxId().equals(event.localTxId())
              && e.type().equals(TxEndedEvent.name()))
          .count() > 0;
    }

    private boolean isCompensated(String globalTxId, TxEvent event) {
      return events.stream()
          .filter(e -> globalTxId.equals(e.globalTxId())
              && e.localTxId().equals(event.localTxId())
              && e.type().equals(TxCompensatedEvent.name()))
          .count() > 0;
    }
  };

  private final List<Command> commands = new ArrayList<>();
  private final CommandRepository commandRepository = new CommandRepository() {
    @Override
    public boolean exists(String globalTxId, String localTxId) {
      return commands.stream()
          .anyMatch(command -> globalTxId.equals(command.globalTxId()) && localTxId.equals(command.localTxId()));
    }

    @Override
    public void saveCompensationCommand(String globalTxId, String localTxId) {
      TxEvent event = eventRepository.findFirstTransaction(globalTxId, localTxId, TxStartedEvent.name());
      commands.add(new Command(event));
    }

    @Override
    public void saveCompensationCommands(String globalTxId) {
      List<TxEvent> events = eventRepository.findTransactionsToCompensate(globalTxId);

      Map<String, Command> commandMap = new HashMap<>();

      for (TxEvent event : events) {
        commandMap.computeIfAbsent(event.localTxId(), k -> new Command(event));
      }

      commands.addAll(commandMap.values());
    }

    @Override
    public void markCommandAsDone(String globalTxId, String localTxId) {
      for (int i = 0; i < commands.size(); i++) {
        Command command = commands.get(i);
        if (globalTxId.equals(command.globalTxId()) && localTxId.equals(command.localTxId())) {
          commands.set(i, new Command(command, DONE));
        }
      }
    }

    @Override
    public List<Command> findUncompletedCommands(String globalTxId) {
      return commands.stream()
          .filter(command -> command.globalTxId().equals(globalTxId) && !DONE.name().equals(command.status()))
          .collect(Collectors.toList());
    }

    @Override
    public List<Command> findFirstCommandToCompensate() {
      List<Command> results = new ArrayList<>(1);
      commands.stream()
          .filter(command -> !DONE.name().equals(command.status()))
          .findFirst()
          .ifPresent(results::add);

      return results;
    }
  };

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final String compensationMethod = getClass().getCanonicalName();
  private final List<CompensationContext> compensationContexts = new ArrayList<>();

  private Consumer<TxEvent> eventConsumer = event -> {};
  private final OmegaCallback omegaCallback = event -> {
    eventConsumer.accept(event);
    compensationContexts.add(
        new CompensationContext(event.globalTxId(), event.localTxId(), event.compensationMethod(), event.payloads()));
  };

  private final TxConsistentService consistentService = new TxConsistentService(
      eventRepository,
      commandRepository,
      omegaCallback,
      300);

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
    assertThat(compensationContexts.isEmpty(), is(true));
  }

  @Test
  public void compensateGlobalTx_OnAnyLocalTxFailure() throws Exception {
    eventConsumer = event -> consistentService
        .handle(eventOf(TxCompensatedEvent, new byte[0], event.localTxId(), event.compensationMethod()));

    String localTxId1 = UUID.randomUUID().toString();
    events.add(eventOf(TxStartedEvent, "service a".getBytes(), localTxId1, "method a"));
    events.add(eventOf(TxEndedEvent, new byte[0], localTxId1, "method a"));

    String localTxId2 = UUID.randomUUID().toString();
    events.add(eventOf(TxStartedEvent, "service b".getBytes(), localTxId2, "method b"));
    events.add(eventOf(TxEndedEvent, new byte[0], localTxId2, "method b"));

    TxEvent abortEvent = newEvent(TxAbortedEvent);

    consistentService.handle(abortEvent);

    await().atMost(1, SECONDS).until(() -> compensationContexts.size() > 1);
    assertThat(compensationContexts, containsInAnyOrder(
        new CompensationContext(globalTxId, localTxId1, "method a", "service a".getBytes()),
        new CompensationContext(globalTxId, localTxId2, "method b", "service b".getBytes())
    ));

    await().atMost(1, SECONDS).until(() -> events.size() == 8);
    assertThat(events.pollLast().type(), is(SagaEndedEvent.name()));
  }

  @Test
  public void compensateTxEndedEventImmediately_IfGlobalTxAlreadyFailed() throws Exception {
    events.add(newEvent(TxStartedEvent));
    events.add(newEvent(TxAbortedEvent));

    TxEvent event = eventOf(TxEndedEvent, new byte[0], localTxId, compensationMethod);

    consistentService.handle(event);

    await().atMost(1, SECONDS).until(() -> compensationContexts.size() > 0);
    assertThat(compensationContexts, containsInAnyOrder(
        new CompensationContext(globalTxId, localTxId, compensationMethod, "yeah".getBytes())
    ));
  }

  @Test
  public void skipTxStartedEvent_IfGlobalTxAlreadyFailed() {
    String localTxId1 = UUID.randomUUID().toString();
    events.add(newEvent(TxStartedEvent));
    events.add(newEvent(TxAbortedEvent));

    TxEvent event = eventOf(TxStartedEvent, "service x".getBytes(), localTxId1, "method x");

    consistentService.handle(event);

    assertThat(events.size(), is(2));
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
