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
import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxConsistentService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Consumer<TxEvent> DO_NOTHING_CONSUMER = event -> {};

  private static final byte[] EMPTY_PAYLOAD = new byte[0];

  private final TxEventRepository eventRepository;
  private final CommandRepository commandRepository;
  private final OmegaCallback omegaCallback;
  private final Map<String, Consumer<TxEvent>> eventCallbacks = new HashMap<String, Consumer<TxEvent>>() {{
    put(TxEndedEvent.name(), (event) -> compensateIfAlreadyAborted(event));
    put(TxAbortedEvent.name(), (event) -> compensate(event));
    put(TxCompensatedEvent.name(), (event) -> updateCompensateStatus(event));
  }};

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  public TxConsistentService(TxEventRepository eventRepository,
      CommandRepository commandRepository,
      OmegaCallback omegaCallback,
      int commandPollingInterval) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.omegaCallback = omegaCallback;

    scheduleCompensationCommandPolling(commandPollingInterval);
  }

  public boolean handle(TxEvent event) {
    if (TxStartedEvent.name().equals(event.type()) && isGlobalTxAborted(event)) {
      return false;
    }

    eventRepository.save(event);

    executor.execute(() -> eventCallbacks.getOrDefault(event.type(), DO_NOTHING_CONSUMER).accept(event));
    return true;
  }

  private void compensateIfAlreadyAborted(TxEvent event) {
    if (!isCompensationScheduled(event) && isGlobalTxAborted(event)) {
      commandRepository.saveCompensationCommand(event.globalTxId(), event.localTxId());
    }
  }

  private boolean isCompensationScheduled(TxEvent event) {
    return commandRepository.exists(event.globalTxId(), event.localTxId());
  }

  private void compensate(TxEvent event) {
    commandRepository.saveCompensationCommands(event.globalTxId());
  }

  // TODO: 2018/1/13 SagaEndedEvent may still not be the last, because some omegas may have slow network and its TxEndedEvent reached late,
  // unless we ask user to specify a name for each participant in the global TX in @Compensable
  private void updateCompensateStatus(TxEvent event) {
    commandRepository.markCommandAsDone(event.globalTxId(), event.localTxId());
    log.info("Transaction with globalTxId {} and localTxId {} was compensated", event.globalTxId(), event.localTxId());

    if (eventRepository.findTransactions(event.globalTxId(), SagaEndedEvent.name()).isEmpty()
        && commandRepository.findUncompletedCommands(event.globalTxId()).isEmpty()) {
      markGlobalTxEnd(event);
      log.info("Marked end of transaction with globalTxId {}", event.globalTxId());
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

  private void scheduleCompensationCommandPolling(int commandPollingInterval) {
    scheduler.scheduleWithFixedDelay(
        () -> commandRepository.findFirstCommandToCompensate()
            .forEach(command -> {
              log.info("Compensating transaction with globalTxId {} and localTxId {}",
                  command.globalTxId(),
                  command.localTxId());

              omegaCallback.compensate(new TxEvent(
                  command.serviceName(),
                  command.instanceId(),
                  command.globalTxId(),
                  command.localTxId(),
                  command.parentTxId(),
                  TxStartedEvent.name(),
                  command.compensationMethod(),
                  command.payloads()
              ));
            }),
        0,
        commandPollingInterval,
        MILLISECONDS);
  }
}
