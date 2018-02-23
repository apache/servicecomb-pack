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

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.TxTimeout;
import org.apache.servicecomb.saga.alpha.core.TxTimeoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

public class SpringTxTimeoutRepository implements TxTimeoutRepository {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxTimeoutEntityRepository timeoutRepo;

  SpringTxTimeoutRepository(TxTimeoutEntityRepository timeoutRepo) {
    this.timeoutRepo = timeoutRepo;
  }

  @Override
  public void save(TxTimeout timeout) {
    try {
      timeoutRepo.save(timeout);
    } catch (Exception ignored) {
      LOG.warn("Failed to save some timeout {}", timeout);
    }
  }

  @Override
  public void markTimeoutAsDone() {
    timeoutRepo.updateStatusOfFinishedTx();
  }

  @Transactional
  @Override
  public List<TxTimeout> findFirstTimeout() {
    List<TxTimeout> timeoutEvents = timeoutRepo.findFirstTimeoutTxOrderByExpireTimeAsc(new PageRequest(0, 1));
    timeoutEvents.forEach(event -> timeoutRepo
        .updateStatusByGlobalTxIdAndLocalTxId(PENDING.name(), event.globalTxId(), event.localTxId()));
    return timeoutEvents;
  }
}