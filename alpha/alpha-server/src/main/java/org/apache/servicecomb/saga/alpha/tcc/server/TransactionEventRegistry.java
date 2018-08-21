/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.servicecomb.saga.alpha.tcc.server;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.saga.alpha.tcc.server.event.ParticipateEvent;

/**
 * Manage TCC transaction event.
 *
 * @author zhaojun
 */
public class TransactionEventRegistry {

  private final static Map<String, List<ParticipateEvent>> TRANS_EVENTS = new ConcurrentHashMap<>();

  public static void register(ParticipateEvent participateEvent) {
    TRANS_EVENTS
        .computeIfAbsent(participateEvent.getGlobalTxId(), key -> new LinkedList<>())
        .add(participateEvent);
  }

  public static List<ParticipateEvent> getTxEvents(String globalTxId) {
    return TRANS_EVENTS.get(globalTxId);
  }
}
