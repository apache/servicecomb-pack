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

package org.apache.servicecomb.saga.alpha.server.tcc.service;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEventHistory;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEventHistoryRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ParticipatedEventServiceImpl implements ParticipatedEventService {

  @Autowired
  private ParticipatedEventRepository hotRepository;

  @Autowired
  private ParticipatedEventHistoryRepository coldRepository;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  @Transactional
  public boolean addEvent(ParticipatedEvent event) {
    try {
      if (!hotRepository.findByUniqueKey(
          event.getGlobalTxId(), event.getLocalTxId()).isPresent()) {
        hotRepository.save(event);
      }
    } catch (Exception ex) {
      LOG.warn("Add participateEvent triggered exception, globalTxId:{}, localTxId:{}, ",
          event.getGlobalTxId(), event.getLocalTxId(), ex);
      return false;
    }
    return true;
  }

  @Override
  public Optional<List<ParticipatedEvent>> getEventByGlobalTxId(String globalTxId) {
    return hotRepository.findByGlobalTxId(globalTxId);
  }

  @Override
  @Transactional
  public void migration(String globalTxId, String localTxId) {
    hotRepository.findByUniqueKey(globalTxId, localTxId).ifPresent( e -> {
      hotRepository.delete(e);
      ParticipatedEventHistory finishedEvent = new ParticipatedEventHistory(
          e.getGlobalTxId(),
          e.getLocalTxId(),
          e.getParentTxId(),
          e.getServiceName(),
          e.getInstanceId(),
          e.getConfirmMethod(),
          e.getCancelMethod(),
          e.getStatus()
      );
      coldRepository.save(finishedEvent);
    });
  }
}
