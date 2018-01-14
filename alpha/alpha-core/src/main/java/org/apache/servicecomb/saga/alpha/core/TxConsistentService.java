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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

public class TxConsistentService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventRepository eventRepository;

  public TxConsistentService(TxEventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public boolean handle(TxEvent event) {
    if (TxStartedEvent.name().equals(event.type()) && isGlobalTxAborted(event)) {
      log.info("Sub-transaction rejected, because its parent with globalTxId {} was already aborted", event.globalTxId());
      return false;
    }

    eventRepository.save(event);

    return true;
  }

//  private void compensate(TxEvent event) {
//    List<TxEvent> events = eventRepository.findTransactionsToCompensate(event.globalTxId());
//
//    Optional<TxEvent> startedEvent = events.stream().filter(e -> e.containChildren(event)).findFirst();
//
//    startedEvent.ifPresent(compensateEvent -> {
//      Integer[] retiesAndTimes = eventsToRetries.compute(event.parentTxId(), (k, v) ->
//          v == null ? new Integer[] {compensateEvent.retries(), 0} : new Integer[] {v[0], v[1] + 1});
//      List<TxEvent> compensationEvents =
//          retiesAndTimes[0] >= retiesAndTimes[1] ? events : Collections.singletonList(
//              new TxEvent(
//                  event.serviceName(), event.instanceId(), event.creationTime(), event.globalTxId(),
//                  event.localTxId(), event.parentTxId(), event.type(), event.retriesMethod(), event.payloads()
//              ));
//
//      compensateImpl(event.globalTxId(), compensationEvents);
//    });
//  }
//
//  private void compensateImpl(String globalTxId, List<TxEvent> events) {
//    events.removeIf(this::isCompensationScheduled);
//
//    Set<String> localTxIds = eventsToCompensate.computeIfAbsent(globalTxId, k -> new HashSet<>());
//    events.forEach(e -> localTxIds.add(e.localTxId()));
//
//    events.forEach(omegaCallback::compensate);
//  }

  // TODO: 2018/1/13 SagaEndedEvent may still not be the last, because some omegas may have slow network and its TxEndedEvent reached late,
  // unless we ask user to specify a name for each participant in the global TX in @Compensable
//  private void updateCompensateStatus(TxEvent event) {
//    Set<String> events = eventsToCompensate.get(event.globalTxId());
//    if (events != null) {
//      events.remove(event.localTxId());
//      if (events.isEmpty()) {
//        eventsToCompensate.remove(event.globalTxId());
//        Integer[] retiesAndTimes = eventsToRetries.get(event.parentTxId());
//        if (retiesAndTimes == null || retiesAndTimes[0] >= retiesAndTimes[1]) {
//          markGlobalTxEnd(event);
//          eventsToRetries.remove(event.parentTxId());
//        }
//      }
//    }
//  }
  private boolean isGlobalTxAborted(TxEvent event) {
    return !eventRepository.findTransactions(event.globalTxId(), TxAbortedEvent.name()).isEmpty();
  }
}
