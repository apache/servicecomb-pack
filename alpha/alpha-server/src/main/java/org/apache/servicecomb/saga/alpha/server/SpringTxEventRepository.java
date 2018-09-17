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

import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;

import java.util.List;
import java.util.Optional;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.springframework.data.domain.PageRequest;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;
import org.springframework.transaction.annotation.Transactional;

@EnableKamon
class SpringTxEventRepository implements TxEventRepository {

  private final TxEventEnvelopeRepository eventRepo;

  SpringTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    this.eventRepo = eventRepo;
  }

  @Override
  @Segment(name = "TxEventSave", category = "application", library = "kamon")
  public void save(TxEvent event) {
    eventRepo.save(event);
  }

  @Override
  @Segment(name = "findFirstAbortedGloableTransaction", category = "application", library = "kamon")
  public Optional<List<TxEvent>> findFirstAbortedGlobalTransaction() {
    return eventRepo.findFirstAbortedGlobalTxByType();
  }

  @Override
  @Segment(name = "findTimeoutEvents", category = "application", library = "kamon")
  public List<TxEvent> findTimeoutEvents() {
    return eventRepo.findTimeoutEvents();
  }

  @Override
  @Segment(name = "findTxStartedEvent", category = "application", library = "kamon")
  public Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId) {
    return eventRepo.findFirstStartedEventByGlobalTxIdAndLocalTxId(globalTxId, localTxId);
  }

  @Override
  @Segment(name = "findTransactions", category = "application", library = "kamon")
  public List<TxEvent> findTransactions(String globalTxId, String type) {
    return eventRepo.findByEventGlobalTxIdAndEventType(globalTxId, type);
  }

  @Override
  @Segment(name = "findNeedToCompensateTxs", category = "application", library = "kamon")
  public List<TxEvent> findNeedToCompensateTxs() {
    return eventRepo.findNeedToCompensateTxs();
  }

  @Override
  @Segment(name = "findAllFinishedTxsForNoTxEnd", category = "application", library = "kamon")
  public List<TxEvent> findAllFinishedTxsForNoTxEnd() {
    return eventRepo.findAllFinishedTxsForNoTxEnd();
  }


  @Override
  @Segment(name = "findCompensatedDoneTxs", category = "application", library = "kamon")
  public List<TxEvent> findCompensatedDoneTxs(String globalTxId, String localTxId) {
    return eventRepo.findCompensatedDoneTxs(globalTxId, localTxId);
  }

  @Override
  public void deleteDuplicateEvents(String type) {
    eventRepo.findDuplicateEventsByType(type).forEach((txEvent) -> eventRepo.
        deleteBySurrogateId(txEvent.id()));
  }

  @Transactional
  @Override
  public void dumpColdEventData() {
    eventRepo.findEventsByType(SagaEndedEvent.name()).forEach(txEvent -> {
      eventRepo.copyToHistoryTable(txEvent.globalTxId());
      eventRepo.deleteByGlobalTxId(txEvent.globalTxId());
    });
  }
}
