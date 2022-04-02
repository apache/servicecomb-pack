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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.EventConverter;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;

public class MemoryTxEventRepository implements TccTxEventRepository {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Set<TccTxEvent>> tccEventMap = new ConcurrentHashMap<>();

  @Override
  public void saveGlobalTxEvent(GlobalTxEvent event) {
    save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  public void saveParticipatedEvent(ParticipatedEvent event) {
    save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  public void updateParticipatedEventStatus(ParticipatedEvent event) {
    save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  public void coordinated(TccTxEvent event) {
  }

  @Override
  public void save(TccTxEvent event) {
    tccEventMap
        .computeIfAbsent(event.getGlobalTxId(), key-> new LinkedHashSet<>())
        .add(event);
  }

  @Override
  public Optional<List<TccTxEvent>> findByGlobalTxId(String globalTxId) {
    Set<TccTxEvent> events = tccEventMap.get(globalTxId);
    if ( events != null) {
      return Optional.of(new ArrayList<>(events));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<List<ParticipatedEvent>> findParticipatedByGlobalTxId(String globalTxId) {
    return Optional.of(
        findByGlobalTxId(globalTxId)
            .orElse(new ArrayList<>()).stream()
        .filter(e -> TccTxType.P_TX_ENDED.name().equals(e.getTxType()))
        .map(EventConverter::convertToParticipatedEvent).collect(Collectors.toList())
    );
  }

  @Override
  public Optional<List<TccTxEvent>> findByGlobalTxIdAndTxType(String globalTxId, TccTxType tccTxType) {
    Set<TccTxEvent> events = tccEventMap.get(globalTxId);
    if ( events != null) {
      return Optional.of(events.stream().filter(e -> tccTxType.name().equals(e.getTxType())).collect(Collectors.toList()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<TccTxEvent> findByUniqueKey(String globalTxId, String localTxId, TccTxType tccTxType) {
    Set<TccTxEvent> events = tccEventMap.get(globalTxId);
    if (events != null) {
      return events.stream().filter(e ->
        tccTxType.name().equals(e.getTxType())
          && localTxId.equals(e.getLocalTxId())).findAny();
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<List<GlobalTxEvent>> findTimeoutGlobalTx(Date deadLine, String txType, Pageable pageable) {
    return Optional.empty();
  }

  @Override
  public void clearCompletedGlobalTx(Pageable pageable) {

  }

  @Override
  public Iterable<TccTxEvent> findAll() {
    List<TccTxEvent> events = new ArrayList<>();
    for (String globalTxId : tccEventMap.keySet()) {
      events.addAll(tccEventMap.get(globalTxId));
    }
    return events;
  }

  @Override
  public void deleteAll() {
    tccEventMap.clear();
  }
}
