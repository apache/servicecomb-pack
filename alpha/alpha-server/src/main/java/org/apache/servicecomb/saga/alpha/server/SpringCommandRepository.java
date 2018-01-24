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

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.CommandRepository;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import javax.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.apache.servicecomb.saga.alpha.core.CommandStatus.*;

public class SpringCommandRepository implements CommandRepository {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final PageRequest SINGLE_COMMAND_REQUEST = new PageRequest(0, 1);

  private final TxEventEnvelopeRepository eventRepository;
  private final CommandEntityRepository commandRepository;

  SpringCommandRepository(TxEventEnvelopeRepository eventRepository, CommandEntityRepository commandRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
  }

  @Override
  public void saveCompensationCommands(TxEvent abortEvent) {
    Optional<TxEvent> compensationStartedEvent =
        eventRepository.findStartedEventWithLocalTxId(abortEvent.globalTxId(), abortEvent.parentTxId());

    compensationStartedEvent.ifPresent(txEvent -> {
      String retriesMethod = txEvent.retriesMethod();
      long retried = retriedTimes(txEvent.globalTxId(), retriesMethod, txEvent.localTxId());

      List<TxEvent> compensationEvents = createRetriesTxEvent(abortEvent.id(), txEvent);

      if (txEvent.retries() < (retried + 1)) {
        compensationEvents =
            eventRepository.findStartedEventsWithMatchingEndedButNotCompensatedEvents(txEvent.globalTxId());
      }

      compensationEvents.stream().map(Command::new)
          .forEach(command -> {
            log.info("Saving compensation command {}", command);
            try {
              commandRepository.save(command);
            } catch (Exception e) {
              log.warn("Failed to save some command {}", command);
            }
            log.info("Saved compensation command {}", command);
          });
    });
  }

  @Override
  public void markCommandAsDone(String globalTxId, String localTxId) {
    commandRepository.updateStatusByGlobalTxIdAndLocalTxId(DONE.name(), globalTxId, localTxId);
  }

  @Override
  public List<Command> findUncompletedCommands(String globalTxId) {
    return commandRepository.findByGlobalTxIdAndStatus(globalTxId, NEW.name());
  }

  @Transactional
  @Override
  public List<Command> findFirstCommandToCompensate() {
    List<Command> commands = commandRepository
        .findFirstGroupByGlobalTxIdWithoutPendingOrderByIdDesc();

    commands.forEach(command ->
        commandRepository.updateStatusByGlobalTxIdAndLocalTxId(
            PENDING.name(),
            command.globalTxId(),
            command.localTxId()));

    return commands;
  }

  private long retriedTimes(String globalTxId, String retriesMethod, String localTxId) {
    return commandRepository.findByGlobalTxIdAndStatus(globalTxId, DONE.name()).stream()
        .filter(c -> Objects.equals(c.compensationMethod(), retriesMethod)
            && Objects.equals(c.localTxId(), localTxId)).count();
  }

  private List<TxEvent> createRetriesTxEvent(long abortEventId, TxEvent txEvent) {
    return Collections.singletonList(new TxEvent(
        abortEventId, txEvent.serviceName(), txEvent.instanceId(), txEvent.creationTime(),
        txEvent.globalTxId(), txEvent.localTxId(), txEvent.parentTxId(),
        txEvent.type(), txEvent.retriesMethod(), txEvent.payloads()
    ));
  }
}
