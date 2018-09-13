/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server.tcc;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Manage TCC transaction event.
 */
@Component("defaultTccTxEventFacade")
public final class DefaultTccTxEventFacadeImpl implements TccTxEventFacade {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Set<GlobalTxEvent>> globalTxMap = new ConcurrentHashMap<>();

  private final Map<String, Set<ParticipatedEvent>> participateMap = new ConcurrentHashMap<>();

  @Override
  public boolean addGlobalTxEvent(GlobalTxEvent globalTxEvent) {
    globalTxMap
        .computeIfAbsent(globalTxEvent.getGlobalTxId(), key -> new LinkedHashSet<>())
        .add(globalTxEvent);

    LOG.info("Registered participated event, global tx: {}, local tx: {}, parent id: {}, "
            + "txType: {}, service [{}] instanceId [{}]",
        globalTxEvent.getGlobalTxId(), globalTxEvent.getLocalTxId(), globalTxEvent.getParentTxId(),
        globalTxEvent.getTxType(), globalTxEvent.getServiceName(), globalTxEvent.getInstanceId());
    return true;
  }

  @Override
  public Set<GlobalTxEvent> getGlobalTxEventByGlobalTxId(String globalTxId) {
    return globalTxMap.get(globalTxId);
  }

  @Override
  public void migrationGlobalTxEvent(String globalTxId, String localTxId) {
    Set<GlobalTxEvent> needRemoveSet = globalTxMap.get(globalTxId).stream()
        .filter((e) -> globalTxId.equals(e.getGlobalTxId()) && localTxId.equals(e.getLocalTxId()))
        .collect(Collectors.toSet());
    globalTxMap.get(globalTxId).removeAll(needRemoveSet);
  }

  /**
   * Register participate event.
   *
   * @param participatedEvent participated event
   */

  @Override
  public boolean addParticipateEvent(ParticipatedEvent participatedEvent) {
    participateMap
        .computeIfAbsent(participatedEvent.getGlobalTxId(), key -> new LinkedHashSet<>())
        .add(participatedEvent);

    LOG.info("Registered participated event, global tx: {}, local tx: {}, parent id: {}, "
            + "confirm: {}, cancel: {}, status: {}, service [{}] instanceId [{}]",
        participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), participatedEvent.getParentTxId(),
        participatedEvent.getConfirmMethod(), participatedEvent.getCancelMethod(), participatedEvent.getStatus(),
        participatedEvent.getServiceName(), participatedEvent.getInstanceId());

    // TODO We need to updated the event which transactionStatus is failed.
    return true;
  }

  /**
   * Retrieve participate event from registry.
   *
   * @param globalTxId global transaction id
   * @return participate events
   */
  @Override
  public Set<ParticipatedEvent> getParticipateEventByGlobalTxId(String globalTxId) {
    return participateMap.get(globalTxId);
  }

  @Override
  public void migrationParticipateEvent(String globalTxId, String localTxId) {
    Set<ParticipatedEvent> needRemoveSet = participateMap.get(globalTxId).stream()
        .filter((e) -> globalTxId.equals(e.getGlobalTxId()) && localTxId.equals(e.getLocalTxId()))
        .collect(Collectors.toSet());
    participateMap.get(globalTxId).removeAll(needRemoveSet);
  }
}
