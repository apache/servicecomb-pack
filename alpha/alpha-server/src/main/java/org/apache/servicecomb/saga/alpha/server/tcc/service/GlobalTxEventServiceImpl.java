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

import com.google.common.collect.Sets;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEventHistory;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEventHistoryRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEventRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEventHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GlobalTxEventServiceImpl implements GlobalTxEventService {

  @Autowired
  private GlobalTxEventRepository hotRepository;

  @Autowired
  private GlobalTxEventHistoryRepository coldRepository;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public boolean addEvent(GlobalTxEvent event) {
    try {
      if (!hotRepository.findByUniqueKey(event.getGlobalTxId(),
          event.getLocalTxId(), event.getTxType()).isPresent()) {
        hotRepository.save(event);
      }
    } catch (Exception ex) {
      LOG.warn("Add globalTxEvent triggered exception, globalTxId:{}, localTxId:{}, txType:{}, ",
          event.getGlobalTxId(), event.getLocalTxId(), event.getTxType(), ex);
      return false;
    }
    return true;
  }

  @Override
  public Set<GlobalTxEvent> getEventByGlobalTxId(String globalTxId) {
    Optional<List<GlobalTxEvent>> list = hotRepository.findByGlobalTxId(globalTxId);
    return list.map(Sets::newHashSet).orElseGet(Sets::newHashSet);
  }

  @Override
  public void migration(String globalTxId, String localTxId) {
    hotRepository.findByGlobalTxId(globalTxId).ifPresent(list ->
      list.forEach((e) -> {
        hotRepository.delete(e.getId());
        GlobalTxEventHistory finishedEvent = new GlobalTxEventHistory(
            e.getGlobalTxId(),
            e.getLocalTxId(),
            e.getParentTxId(),
            e.getServiceName(),
            e.getInstanceId(),
            e.getTxType(),
            e.getCreationTime(),
            e.getLastModified()
        );
        coldRepository.save(finishedEvent);
      })
    );
  }
}
