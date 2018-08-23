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

package org.apache.servicecomb.saga.alpha.tcc.server;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.saga.alpha.tcc.server.event.ParticipatedEvent;

/**
 * Manage TCC transaction event.
 */
public final class TransactionEventRegistry {

  private final static Map<String, List<ParticipatedEvent>> REGISTRY = new ConcurrentHashMap<>();

  /**
   * Register participate event.
   *
   * @param participateEvent participate event
   */
  public static void register(ParticipatedEvent participateEvent) {
    REGISTRY
        .computeIfAbsent(participateEvent.getGlobalTxId(), key -> new LinkedList<>())
        .add(participateEvent);
  }

  /**
   * Retrieve participate event from registry.
   *
   * @param globalTxId global transaction id
   * @return participate events
   */
  public static List<ParticipatedEvent> retrieve(String globalTxId) {
    return REGISTRY.get(globalTxId);
  }
}
