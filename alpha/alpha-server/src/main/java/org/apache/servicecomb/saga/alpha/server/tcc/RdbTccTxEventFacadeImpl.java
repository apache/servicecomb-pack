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

import java.lang.invoke.MethodHandles;
import java.util.Set;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.service.ParticipateEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("rdbTccTxEventFacade")
public class RdbTccTxEventFacadeImpl implements TccTxEventFacade {

  @Autowired
  private ParticipateEventService participateEventService;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public boolean addParticipateEvent(ParticipatedEvent event) {
    return participateEventService.addEvent(event);
  }

  @Override
  public Set<ParticipatedEvent> getParticipateEventByGlobalTxId(String globalTxId) {
    return participateEventService.getEventByGlobalTxId(globalTxId);
  }

  @Override
  public void migrationParticipateEvent(String globalTxId, String localTxId) {
    participateEventService.migration(globalTxId, localTxId);
  }
}
