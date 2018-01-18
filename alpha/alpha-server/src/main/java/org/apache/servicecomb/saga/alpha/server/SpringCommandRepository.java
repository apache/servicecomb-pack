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

import static org.apache.servicecomb.saga.alpha.core.CommandStatus.DONE;
import static org.apache.servicecomb.saga.alpha.core.CommandStatus.NEW;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.CommandRepository;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.domain.PageRequest;

public class SpringCommandRepository implements CommandRepository {
  private static final PageRequest SINGLE_COMMAND_REQUEST = new PageRequest(0, 1);

  private final TxEventEnvelopeRepository eventRepository;
  private final CommandEntityRepository commandRepository;

  SpringCommandRepository(TxEventEnvelopeRepository eventRepository, CommandEntityRepository commandRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
  }

  @Override
  public boolean exists(String globalTxId, String localTxId) {
    return commandRepository.findByGlobalTxIdAndLocalTxId(globalTxId, localTxId).isPresent();
  }

  @Override
  public void saveCompensationCommand(String globalTxId, String localTxId) {
    TxEvent startedEvent = eventRepository.findFirstByGlobalTxIdAndLocalTxIdAndTypeOrderBySurrogateIdAsc(
        globalTxId,
        localTxId,
        TxStartedEvent.name());

    commandRepository.save(new Command(startedEvent));
  }

  @Override
  public void saveCompensationCommands(String globalTxId) {
    List<TxEvent> events = eventRepository
        .findStartedEventEnvelopesWithMatchingEndedButNotCompensatedEvents(globalTxId);

    Map<String, Command> commands = new LinkedHashMap<>();

    for (TxEvent event : events) {
      commands.computeIfAbsent(event.localTxId(), k -> new Command(event));
    }

    commandRepository.save(commands.values());
  }

  @Override
  public void markCommandAsDone(String globalTxId, String localTxId) {
    commandRepository.updateStatusByGlobalTxIdAndLocalTxId(DONE.name(), globalTxId, localTxId);
  }

  @Override
  public List<Command> findUncompletedCommands(String globalTxId) {
    return commandRepository.findByGlobalTxIdAndStatus(globalTxId, NEW.name());
  }

  @Override
  public List<Command> findFirstCommandToCompensate() {
    return commandRepository
        .findFirstGroupByGlobalTxIdOrderByIdDesc(SINGLE_COMMAND_REQUEST);
  }
}
