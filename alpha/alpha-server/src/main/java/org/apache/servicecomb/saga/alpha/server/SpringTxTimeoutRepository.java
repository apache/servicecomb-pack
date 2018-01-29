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
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxTimeout;
import org.apache.servicecomb.saga.alpha.core.TxTimeoutRepository;
import org.springframework.data.domain.PageRequest;

public class SpringTxTimeoutRepository implements TxTimeoutRepository {
  private final TxTimeoutEntityRepository timeoutRepo;

  SpringTxTimeoutRepository(TxTimeoutEntityRepository timeoutRepo) {
    this.timeoutRepo = timeoutRepo;
  }

  @Override
  public void save(TxTimeout event) {
    timeoutRepo.save(event);
  }

  @Override
  public void markTxTimeoutAsDone(String globalTxId, String localTxId) {
    timeoutRepo.updateStatusByGlobalTxIdAndLocalTxId(DONE.name(), globalTxId, localTxId);
  }

  @Transactional
  @Override
  public List<TxEvent> findFirstTimeoutTxToAbort() {
    List<TxEvent> timeoutEvents = timeoutRepo.findFirstTimeoutTxOrderByExpireTimeAsc(new PageRequest(0, 1));
    List<TxEvent> pendingTimeoutEvents = new ArrayList<>();
    timeoutEvents.forEach(event -> {
      if (timeoutRepo.updateStatusFromNewByGlobalTxIdAndLocalTxId(PENDING.name(), event.globalTxId(), event.localTxId())
          != 0) {
        pendingTimeoutEvents.add(event);
      }
    });
    return pendingTimeoutEvents;
  }
}