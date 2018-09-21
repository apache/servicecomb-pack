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

package org.apache.servicecomb.saga.alpha.server.tcc.service;

import java.util.List;
import java.util.Optional;

import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEventRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEventRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEventDBRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxType;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.EventConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!memory")
public class RDBTxEventRepository implements TccTxEventRepository {

  @Autowired
  GlobalTxEventRepository globalTxEventRepository;

  @Autowired
  ParticipatedEventRepository participatedEventRepository;

  @Autowired
  TccTxEventDBRepository tccTxEventDBRepository;

  @Override
  public void saveGlobalTxEvent(GlobalTxEvent event) {
    globalTxEventRepository.save(event);
    // saveTccEventHere
    tccTxEventDBRepository.save(EventConverter.convertToTccTxEvent(event));
  }

  @Override
  public void saveParticipatedEvent(ParticipatedEvent event) {
    participatedEventRepository.save(event);
    // saveTccEventHere
    tccTxEventDBRepository.save(EventConverter.convertToTccTxEvent(event));
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
  public Optional<List<TccTxEvent>> findByGlobalTxIdAndTxType(String globalTxId, TccTxType tccTxType) {
    return tccTxEventDBRepository.findByGlobalTxIdAndTxType(globalTxId, tccTxType.name());
  }

  @Override
  public Optional<TccTxEvent> findByUniqueKey(String globalTxId, String localTxId, TccTxType tccTxType) {
    return tccTxEventDBRepository.findByUniqueKey(globalTxId, localTxId, tccTxType.name());
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
