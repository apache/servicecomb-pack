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

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.DONE;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.CommandRepository;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class SpringCommandRepository implements CommandRepository {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventEnvelopeRepository eventRepository;
  private final CommandEntityRepository commandRepository;

  SpringCommandRepository(TxEventEnvelopeRepository eventRepository, CommandEntityRepository commandRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
  }

  @Override
  @Segment(name = "saveCompensationCommands", category = "application", library = "kamon")
  public void saveCompensationCommands(String globalTxId, String localTxId) {

    eventRepository.findLastStartedEvent(globalTxId, localTxId).forEach(event -> {
      Command command = new Command(event);
      try {
        commandRepository.save(command);
      } catch (Exception e) {
        LOG.warn("Failed to save some command {}", command);
      }
    });
  }

  @Override
  @Segment(name = "markCommandAsDone", category = "application", library = "kamon")
  public void markCommandAsDone(String globalTxId, String localTxId) {
    commandRepository.updateStatusByGlobalTxIdAndLocalTxId(DONE.name(), globalTxId, localTxId);
  }

  @Override
  @Segment(name = "markCommandAsPending", category = "application", library = "kamon")
  public void markCommandAsPending(String globalTxId, String localTxId) {
    commandRepository.updateStatusByGlobalTxIdAndLocalTxId(PENDING.name(), globalTxId, localTxId);
  }

  @Override
  @Segment(name = "findUncompletedCommands", category = "application", library = "kamon")
  public List<Command> findUncompletedCommands(String globalTxId) {
    return commandRepository.findUnfinishedCommandByGlobalTxId(globalTxId);
  }

  @Override
  @Segment(name = "findPendingCommands", category = "application", library = "kamon")
  public List<Command> findPendingCommands() {
    return commandRepository.findPendingCommands();
  }

  @Override
  @Segment(name = "findAllCommandsToCompensate", category = "application", library = "kamon")
  public List<Command> findAllCommandsToCompensate() {
    return commandRepository.findNewCommands();
  }
}
