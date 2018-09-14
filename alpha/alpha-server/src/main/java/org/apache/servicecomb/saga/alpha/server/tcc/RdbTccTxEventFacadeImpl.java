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

package org.apache.servicecomb.saga.alpha.server.tcc;

import com.google.common.collect.Lists;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.TccCallbackEngine;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.service.GlobalTxEventService;
import org.apache.servicecomb.saga.alpha.server.tcc.service.ParticipatedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("rdbTccTxEventFacade")
public class RdbTccTxEventFacadeImpl implements TccTxEventFacade {

  @Autowired
  private ParticipatedEventService participatedEventService;

  @Autowired
  private GlobalTxEventService globalTxEventService;

  @Autowired
  @Qualifier("rdbCallbackEngine")
  private TccCallbackEngine tccCallbackEngine;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public boolean onTccStartEvent(GlobalTxEvent globalTxEvent) {
    return globalTxEventService.addEvent(globalTxEvent);
  }

  @Override
  public boolean onParticipateEvent(ParticipatedEvent event) {
    return participatedEventService.addEvent(event);
  }

  @Override
  public boolean onTccEndEvent(GlobalTxEvent globalTxEvent) {
    if (globalTxEventService.addEvent(globalTxEvent)) {
      return tccCallbackEngine.execute(globalTxEvent);
    }
    return false;
  }

  @Override
  public void onCoordinatedEvent(String globalTxId, String localTxId) {
    participatedEventService.migration(globalTxId, localTxId);
  }

  @Override
  public List<GlobalTxEvent> getGlobalTxEventByGlobalTxId(String globalTxId) {
    return globalTxEventService.getEventByGlobalTxId(globalTxId).orElse(Lists.newArrayList());
  }

  @Override
  public List<ParticipatedEvent> getParticipateEventByGlobalTxId(String globalTxId) {
    return participatedEventService.getEventByGlobalTxId(globalTxId).orElse(Lists.newArrayList());
  }

  @Override
  public void migrationGlobalTxEvent(String globalTxId, String localTxId) {
    globalTxEventService.migration(globalTxId, localTxId);
  }
}
