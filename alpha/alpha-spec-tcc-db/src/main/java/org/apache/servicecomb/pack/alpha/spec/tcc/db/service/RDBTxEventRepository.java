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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.EventConverter;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEventRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEventDBRepository;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class RDBTxEventRepository implements TccTxEventRepository {

  @Autowired
  GlobalTxEventRepository globalTxEventRepository;

  @Autowired
  ParticipatedEventRepository participatedEventRepository;

  @Autowired
  TccTxEventDBRepository tccTxEventDBRepository;

  @Override
  @Transactional
  public void saveGlobalTxEvent(GlobalTxEvent event) {
    globalTxEventRepository.save(event);
    // saveTccEventHere
    tccTxEventDBRepository.save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  @Transactional
  public void saveParticipatedEvent(ParticipatedEvent event) {
    // we dont need to save participation-started event to the participatedEventRepository
    // saveTccEventHere
    tccTxEventDBRepository.save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  @Transactional
  public void updateParticipatedEventStatus(ParticipatedEvent event) {
    participatedEventRepository.save(event);
    // saveTccEventHere
    tccTxEventDBRepository.save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  @Transactional
  public void coordinated(TccTxEvent event) {
    participatedEventRepository.findByUniqueKey(event.getGlobalTxId(), event.getLocalTxId()).ifPresent((e) -> {
      participatedEventRepository.delete(e);
      tccTxEventDBRepository.save(event);
    });
  }

  @Override
  public void save(TccTxEvent event) {
    tccTxEventDBRepository.save(event);
  }

  @Override
  public Optional<List<TccTxEvent>> findByGlobalTxId(String globalTxId) {
    return tccTxEventDBRepository.findByGlobalTxId(globalTxId);
  }

  @Override
  public Optional<List<ParticipatedEvent>> findParticipatedByGlobalTxId(String globalTxId) {
    return participatedEventRepository.findByGlobalTxId(globalTxId);
  }

  @Override
  public Optional<List<TccTxEvent>> findByGlobalTxIdAndTxType(String globalTxId, TccTxType tccTxType) {
    return tccTxEventDBRepository.findByGlobalTxIdAndTxType(globalTxId, tccTxType.name());
  }

  @Override
  public Optional<TccTxEvent> findByUniqueKey(String globalTxId, String localTxId, TccTxType tccTxType) {
    return tccTxEventDBRepository.findByUniqueKey(globalTxId, localTxId, tccTxType.name());
  }

  @Override
  public Optional<List<GlobalTxEvent>> findTimeoutGlobalTx(Date deadLine, String txType, Pageable pageable) {
    return globalTxEventRepository.findTimeoutGlobalTx(deadLine, txType, pageable);
  }

  @Override
  public void clearCompletedGlobalTx(Pageable pageable) {
    globalTxEventRepository.findCompletedGlobalTx(pageable).ifPresent(e -> e.forEach(t ->
        globalTxEventRepository.deleteByGlobalId(t)));
    }

  @Override
  public Iterable<TccTxEvent> findAll() {
    return tccTxEventDBRepository.findAll();
  }

  @Override
  public void deleteAll() {
    globalTxEventRepository.deleteAll();
    participatedEventRepository.deleteAll();
    tccTxEventDBRepository.deleteAll();
  }
}
