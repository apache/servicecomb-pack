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

import java.util.List;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;

public interface TccTxEventFacade {

  boolean onTccStartEvent(GlobalTxEvent globalTxEvent);

  boolean onParticipateEvent(ParticipatedEvent participateEvent);

  boolean onTccEndEvent(GlobalTxEvent globalTxEvent);

  void onCoordinatedEvent(String globalTxId, String localTxId);

  List<GlobalTxEvent> getGlobalTxEventByGlobalTxId(String globalTxId);

  List<ParticipatedEvent> getParticipateEventByGlobalTxId(String globalTxId);

//  void migrationParticipateEvent(String globalTxId, String localTxId);
//
  void migrationGlobalTxEvent(String globalTxId, String localTxId);
}
