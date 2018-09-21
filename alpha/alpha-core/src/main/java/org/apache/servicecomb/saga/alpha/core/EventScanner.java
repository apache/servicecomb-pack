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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;
import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import kamon.annotation.EnableKamon;
import kamon.annotation.Trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EnableKamon
public class EventScanner implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final byte[] EMPTY_PAYLOAD = new byte[0];

  private final ScheduledExecutorService scheduler;

  private final TxEventRepository eventRepository;

  private final CommandRepository commandRepository;

  private final TxTimeoutRepository timeoutRepository;

  private final OmegaCallback omegaCallback;

  private final int eventPollingInterval;


  public EventScanner(ScheduledExecutorService scheduler,
      TxEventRepository eventRepository,
      CommandRepository commandRepository,
      TxTimeoutRepository timeoutRepository,
      OmegaCallback omegaCallback,
      int eventPollingInterval) {
    this.scheduler = scheduler;
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.timeoutRepository = timeoutRepository;
    this.omegaCallback = omegaCallback;
    this.eventPollingInterval = eventPollingInterval;
  }

  @Override
  public void run() {
    pollEvents();
  }

  private void pollEvents() {
    scheduler.scheduleWithFixedDelay(
        () -> {
          updateTimeoutStatus();
          findAllTimeoutEvents();
          abortTimeoutEvents();
          saveUncompensatedEventsToCommands();
          compensate();
          updateCompensatedCommands();
          markSagaEndedForNoTxEnd();
          deleteDuplicateSagaEndedEvents();
          dumpColdData();
        },
        0,
        eventPollingInterval,
        MILLISECONDS);
  }

  private void updateTimeoutStatus() {
    timeoutRepository.markTimeoutAsDone();
  }

  @Trace("findAllTimeoutEvents")
  private void findAllTimeoutEvents() {
    eventRepository.findTimeoutEvents()
        .forEach(event -> {
          LOG.info("Found timeout event {}", event);
          timeoutRepository.save(txTimeoutOf(event));
        });
  }

  @Trace("abortTimeoutEvents")
  private void abortTimeoutEvents() {
    timeoutRepository.findTimeouts().forEach(timeout -> {
      LOG.info("Found timeout event {} to abort", timeout);
      eventRepository.save(toTxAbortedEvent(timeout));
    });
  }

  @Trace("saveUncompensatedEventsToCommands")
  private void saveUncompensatedEventsToCommands() {
    eventRepository.findNeedToCompensateTxs()
        .forEach(event -> {
          LOG.info("Found uncompensated event {}", event);
          commandRepository.saveCompensationCommands(event.globalTxId(), event.localTxId());
        });
  }

  @Trace("compensate")
  private void compensate() {
    List<TxEvent> compensateTxEvents = new ArrayList<>();
    commandRepository.findAllCommandsToCompensate()
        .forEach(command ->
            compensateTxEvents.add(txStartedEventOf(command))
        );
    omegaCallback.compensateAllEvents(compensateTxEvents).forEach(
        event -> commandRepository.markCommandAsPending(event.globalTxId(), event.localTxId()));
  }

  private void markSagaEnded(TxEvent event) {
    if (commandRepository.findUncompletedCommands(event.globalTxId()).isEmpty()) {
      LOG.info("Marked end of transaction with globalTxId {}", event.globalTxId());
      markGlobalTxEndWithEvent(event);
    }
  }

  private void updateCompensationStatus(TxEvent event) {
    commandRepository.markCommandAsDone(event.globalTxId(), event.localTxId());
    LOG.info("Transaction with globalTxId {} and localTxId {} was compensated",
        event.globalTxId(),
        event.localTxId());
    markSagaEnded(event);
  }

  @Trace("updateCompensatedCommands")
  private void updateCompensatedCommands() {
    commandRepository.findPendingCommands().forEach(command ->
        eventRepository.findCompensatedDoneTxs(command.globalTxId(), command.localTxId())
            .forEach(event ->
            {
              LOG.info("Found compensated event {}", event);
              updateCompensationStatus(event);
            }));
  }

  private void markGlobalTxEndWithEvent(TxEvent event) {
    eventRepository.save(toSagaEndedEvent(event));
  }

  private void markSagaEndedForNoTxEnd() {
    eventRepository.findAllFinishedTxsForNoTxEnd().forEach(
        event -> {
          LOG.info("Marked end of no tx end's transaction with globalTxId {}", event.globalTxId());
          markGlobalTxEndWithEvent(event);
        });
  }

  @Trace("deleteDuplicateSagaEndedEvents")
  private void deleteDuplicateSagaEndedEvents() {
    try {
      eventRepository.deleteDuplicateEvents(SagaEndedEvent.name());
    } catch (Exception e) {
      LOG.warn("Failed to delete duplicate event", e);
    }
  }

  private void dumpColdData() {
    eventRepository.dumpColdEventData();
  }


  private TxEvent toTxAbortedEvent(TxTimeout timeout) {
    return new TxEvent(
        timeout.serviceName(),
        timeout.instanceId(),
        timeout.globalTxId(),
        timeout.localTxId(),
        timeout.parentTxId(),
        TxAbortedEvent.name(),
        "",
        ("Transaction timeout").getBytes());
  }

  private TxEvent toSagaEndedEvent(TxEvent event) {
    return new TxEvent(
        event.serviceName(),
        event.instanceId(),
        event.globalTxId(),
        event.globalTxId(),
        null,
        SagaEndedEvent.name(),
        "",
        EMPTY_PAYLOAD);
  }


  private TxEvent txStartedEventOf(Command command) {
    return new TxEvent(
        command.serviceName(),
        command.instanceId(),
        command.globalTxId(),
        command.localTxId(),
        command.parentTxId(),
        TxStartedEvent.name(),
        command.compensationMethod(),
        command.payloads());
  }

  private TxTimeout txTimeoutOf(TxEvent event) {
    return new TxTimeout(
        event.id(),
        event.serviceName(),
        event.instanceId(),
        event.globalTxId(),
        event.localTxId(),
        event.parentTxId(),
        event.type(),
        event.expiryTime(),
        NEW.name());
  }
}
