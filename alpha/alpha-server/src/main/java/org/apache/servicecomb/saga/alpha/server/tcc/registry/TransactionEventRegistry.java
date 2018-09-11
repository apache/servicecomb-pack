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

package org.apache.servicecomb.saga.alpha.server.tcc.registry;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.saga.alpha.server.tcc.event.ParticipatedEvent;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage TCC transaction event.
 */
public final class TransactionEventRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final static Map<String, Set<ParticipatedEvent>> REGISTRY = new ConcurrentHashMap<>();

  /**
   * Register participate event.
   *
   * @param participatedEvent participated event
   */
  public static void register(ParticipatedEvent participatedEvent) {
    // Only register the Succeed participatedEvent
    if (TransactionStatus.Succeed.equals(participatedEvent.getStatus())) {
      REGISTRY
          .computeIfAbsent(participatedEvent.getGlobalTxId(), key -> new LinkedHashSet<>())
          .add(participatedEvent);

      LOG.info("Registered participated event, global tx: {}, local tx: {}, parent id: {}, "
              + "confirm: {}, cancel: {}, status: {}, service [{}] instanceId [{}]",
          participatedEvent.getGlobalTxId(), participatedEvent.getLocalTxId(), participatedEvent.getParentTxId(),
          participatedEvent.getConfirmMethod(), participatedEvent.getCancelMethod(), participatedEvent.getStatus(),
          participatedEvent.getServiceName(), participatedEvent.getInstanceId());
    }
  }

  /**
   * Retrieve participate event from registry.
   *
   * @param globalTxId global transaction id
   * @return participate events
   */
  public static Set<ParticipatedEvent> retrieve(String globalTxId) {
    return REGISTRY.get(globalTxId);
  }
}
