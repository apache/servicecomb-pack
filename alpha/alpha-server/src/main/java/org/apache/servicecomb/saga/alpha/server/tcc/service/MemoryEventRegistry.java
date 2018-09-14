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

import com.google.common.collect.Lists;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryEventRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final static Map<String, Set<GlobalTxEvent>> globalTxMap = new ConcurrentHashMap<>();

  private final static Map<String, Set<ParticipatedEvent>> participateMap = new ConcurrentHashMap<>();

  public static void addGlobalTxEvent(GlobalTxEvent globalTxEvent) {
    globalTxMap
        .computeIfAbsent(globalTxEvent.getGlobalTxId(), key -> new LinkedHashSet<>())
        .add(globalTxEvent);
  }

  public static void addParticipateEvent(ParticipatedEvent participatedEvent) {
    participateMap
        .computeIfAbsent(participatedEvent.getGlobalTxId(), key -> new LinkedHashSet<>())
        .add(participatedEvent);
  }

  public static void migrateParticipate(String globalTxId, String localTxId) {
    Set<ParticipatedEvent> needRemoveSet = participateMap.get(globalTxId).stream()
        .filter((e) -> globalTxId.equals(e.getGlobalTxId()) && localTxId.equals(e.getLocalTxId()))
        .collect(Collectors.toSet());
    participateMap.get(globalTxId).removeAll(needRemoveSet);
  }

  public static List<GlobalTxEvent> getGlobalTxEventByGlobalTxId(String globalTxId) {
    Set<GlobalTxEvent> events = globalTxMap.get(globalTxId);
    return null == events ? Lists.newArrayList() : Lists.newArrayList(events);
  }

  public static List<ParticipatedEvent> getParticipateEventByGlobalTxId(String globalTxId) {
    Set<ParticipatedEvent> events = participateMap.get(globalTxId);
    return null == events ? Lists.newArrayList() : Lists.newArrayList(events);
  }

  public static void migrationGlobalTxEvent(String globalTxId, String localTxId) {
    Set<GlobalTxEvent> needRemoveSet = globalTxMap.get(globalTxId).stream()
        .filter((e) -> globalTxId.equals(e.getGlobalTxId()) && localTxId.equals(e.getLocalTxId()))
        .collect(Collectors.toSet());
    globalTxMap.get(globalTxId).removeAll(needRemoveSet);
  }



}
