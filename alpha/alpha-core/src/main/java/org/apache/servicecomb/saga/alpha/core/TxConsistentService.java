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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.SagaStartedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxConsistentService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventRepository eventRepository;
  private final TxTimeoutRepository timeoutRepository;

  public TxConsistentService(TxEventRepository eventRepository,
      TxTimeoutRepository timeoutRepository) {
    this.eventRepository = eventRepository;
    this.timeoutRepository = timeoutRepository;
  }

  public boolean handle(TxEvent event) {
    if (TxStartedEvent.name().equals(event.type()) && isGlobalTxAborted(event)) {
      log.info("Sub-transaction rejected, because its parent with globalTxId {} was already aborted", event.globalTxId());
      return false;
    }

    if (isEventWithTimeout(event)) {
      CompletableFuture.runAsync(() -> saveTxTimeout(event));
    }

    eventRepository.save(event);

    if (Arrays.asList(TxEndedEvent.name(), SagaEndedEvent.name(), TxAbortedEvent.name()).contains(event.type())) {
      CompletableFuture.runAsync(() -> timeoutRepository.markTxTimeoutAsDone(event.globalTxId(), event.localTxId()));
    }

    return true;
  }

  private boolean isEventWithTimeout(TxEvent event) {
    return Arrays.asList(TxStartedEvent.name(), SagaStartedEvent.name()).contains(event.type()) && event.timeout() != 0;
  }

  private void saveTxTimeout(TxEvent event) {
    Date expireTime = new Date(event.creationTime().getTime() + SECONDS.toMillis(event.timeout()));
    timeoutRepository.save(
        new TxTimeout(
            event.globalTxId(),
            event.localTxId(),
            expireTime,
            NEW.name()));
  }

  private boolean isGlobalTxAborted(TxEvent event) {
    return !eventRepository.findTransactions(event.globalTxId(), TxAbortedEvent.name()).isEmpty();
  }
}
