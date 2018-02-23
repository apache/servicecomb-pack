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

import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;

import java.util.List;
import java.util.Optional;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.springframework.data.domain.PageRequest;

class SpringTxEventRepository implements TxEventRepository {
  private static final PageRequest SINGLE_TX_EVENT_REQUEST = new PageRequest(0, 1);
  private final TxEventEnvelopeRepository eventRepo;

  SpringTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    this.eventRepo = eventRepo;
  }

  @Override
  public void save(TxEvent event) {
    eventRepo.save(event);
  }

  @Override
  public Optional<TxEvent> findFirstAbortedGlobalTransaction() {
    return eventRepo.findFirstAbortedGlobalTxByType();
  }

  @Override
  public List<TxEvent> findTimeoutEvents() {
    return eventRepo.findTimeoutEvents(SINGLE_TX_EVENT_REQUEST);
  }

  @Override
  public Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId) {
    return eventRepo.findFirstStartedEventByGlobalTxIdAndLocalTxId(globalTxId, localTxId);
  }

  @Override
  public List<TxEvent> findTransactions(String globalTxId, String type) {
    return eventRepo.findByEventGlobalTxIdAndEventType(globalTxId, type);
  }

  @Override
  public List<TxEvent> findFirstUncompensatedEventByIdGreaterThan(long id, String type) {
    return eventRepo.findFirstByTypeAndSurrogateIdGreaterThan(type, id, SINGLE_TX_EVENT_REQUEST);
  }

  @Override
  public Optional<TxEvent> findFirstCompensatedEventByIdGreaterThan(long id) {
    return eventRepo.findFirstByTypeAndSurrogateIdGreaterThan(TxCompensatedEvent.name(), id);
  }

  @Override
  public void deleteDuplicateEvents(String type) {
    eventRepo.deleteByType(type);
  }
}
