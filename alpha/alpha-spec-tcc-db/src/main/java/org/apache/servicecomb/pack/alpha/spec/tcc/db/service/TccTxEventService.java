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

import java.lang.invoke.MethodHandles;
import java.util.Date;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.TccCallbackEngine;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

public class TccTxEventService {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TccTxEventRepository tccTxEventRepository;
  private final TccCallbackEngine tccCallbackEngine;

  public TccTxEventService(
      TccTxEventRepository tccTxEventRepository,
      TccCallbackEngine tccCallbackEngine) {
    this.tccTxEventRepository = tccTxEventRepository;
    this.tccCallbackEngine = tccCallbackEngine;
  }

  public boolean onTccStartedEvent(GlobalTxEvent globalTxEvent) {
    LOG.info("Registered TccStarted event, global tx: {}, local tx: {}, parent id: {}, "
            + "txType: {}, service [{}] instanceId [{}]",
        globalTxEvent.getGlobalTxId(), globalTxEvent.getLocalTxId(), globalTxEvent.getParentTxId(),
        globalTxEvent.getTxType(), globalTxEvent.getServiceName(), globalTxEvent.getInstanceId());
    try {
      if (!tccTxEventRepository.findByUniqueKey(globalTxEvent.getGlobalTxId(), globalTxEvent.getLocalTxId(),
          TccTxType.valueOf(globalTxEvent.getTxType())).isPresent()) {
        tccTxEventRepository.saveGlobalTxEvent(globalTxEvent);
      }
    } catch (Exception ex) {
      LOG.warn("Add globalTxEvent triggered exception, globalTxId:{}, localTxId:{}, txType:{}, ",
          globalTxEvent.getGlobalTxId(), globalTxEvent.getLocalTxId(), globalTxEvent.getTxType(), ex);
      return false;
    }
    return true;
  }

  public boolean onParticipationStartedEvent(ParticipatedEvent participatedEvent) {
    LOG.info("Registered Participation started event, global tx: {}, local tx: {}, parent id: {}, "
            + "confirm: {}, cancel: {}, status: {}, service [{}] instanceId [{}]",
        participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), participatedEvent.getParentTxId(),
        participatedEvent.getConfirmMethod(), participatedEvent.getCancelMethod(), participatedEvent.getStatus(),
        participatedEvent.getServiceName(), participatedEvent.getInstanceId());
    try {
      if (!tccTxEventRepository.findByUniqueKey(participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), TccTxType.P_TX_STATED).isPresent()) {
        tccTxEventRepository.saveParticipatedEvent(participatedEvent);
      }
    } catch (Exception ex) {
      LOG.warn("Add participateEvent triggered exception, globalTxId:{}, localTxId:{}, ",
          participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), ex);
      return false;
    }
    return true;
  }

  public boolean onParticipationEndedEvent(ParticipatedEvent participatedEvent) {
    LOG.info("Registered Participation ended event, global tx: {}, local tx: {}, parent id: {}, "
                    + "confirm: {}, cancel: {}, status: {}, service [{}] instanceId [{}]",
            participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), participatedEvent.getParentTxId(),
            participatedEvent.getConfirmMethod(), participatedEvent.getCancelMethod(), participatedEvent.getStatus(),
            participatedEvent.getServiceName(), participatedEvent.getInstanceId());
    try {
      if (!tccTxEventRepository.findByUniqueKey(participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), TccTxType.P_TX_ENDED).isPresent()) {
        tccTxEventRepository.updateParticipatedEventStatus(participatedEvent);
      }
    } catch (Exception ex) {
      LOG.warn("Add participateEvent triggered exception, globalTxId:{}, localTxId:{}, ",
              participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), ex);
      return false;
    }
    return true;
  }

  public boolean onTccEndedEvent(GlobalTxEvent globalTxEvent) {
    LOG.info("Registered TccEnded event, global tx: {}, local tx: {}, parent id: {}, "
            + "txType: {}, service [{}] instanceId [{}]",
        globalTxEvent.getGlobalTxId(), globalTxEvent.getLocalTxId(), globalTxEvent.getParentTxId(),
        globalTxEvent.getTxType(), globalTxEvent.getServiceName(), globalTxEvent.getInstanceId());
    try {
      tccTxEventRepository.saveGlobalTxEvent(globalTxEvent);
    } catch (Exception ex) {
      LOG.warn("Add globalTxEvent triggered exception, globalTxId:{}, localTxId:{}, txType:{}, ",
          globalTxEvent.getGlobalTxId(), globalTxEvent.getLocalTxId(), globalTxEvent.getTxType(), ex);
      return false;
    }
    // Just return the execution result back
    return tccCallbackEngine.execute(globalTxEvent);
  }

  public boolean onCoordinatedEvent(TccTxEvent tccTxEvent) {
    LOG.info("Registered Coordinate event, global tx: {}, local tx: {}, parent id: {}, "
            + "txType: {}, service [{}] instanceId [{}]",
        tccTxEvent.getGlobalTxId(), tccTxEvent.getLocalTxId(), tccTxEvent.getParentTxId(),
        tccTxEvent.getTxType(), tccTxEvent.getServiceName(), tccTxEvent.getInstanceId());
    try {
      tccTxEventRepository.coordinated(tccTxEvent);
    } catch (Exception ex) {
      LOG.warn("Add coordinatedEvent triggered exception, globalTxId:{}, localTxId:{} ",
          tccTxEvent.getGlobalTxId(), tccTxEvent.getLocalTxId(), ex);
      return false;
    }
    return true;
  }

  public void handleTimeoutTx(Date deadLine, int size) {
    tccTxEventRepository.findTimeoutGlobalTx(deadLine, TccTxType.STARTED.name(), PageRequest.of(0, size))
        .ifPresent(e -> e.forEach(t -> {
          GlobalTxEvent globalTxEvent = new GlobalTxEvent(
              t.getServiceName(),
              t.getInstanceId(),
              t.getGlobalTxId(),
              t.getLocalTxId(),
              t.getParentTxId(),
              TccTxType.END_TIMEOUT.name(),
              TransactionStatus.Failed.name());
          onTccEndedEvent(globalTxEvent);
        }));
  }

  public void clearCompletedGlobalTx(int size) {
    tccTxEventRepository.clearCompletedGlobalTx(PageRequest.of(0, size));
  }
}
