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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;

public class TxEntities {

  private LinkedHashMap<String, TxEntity> entities = new LinkedHashMap<>();

  public void forEach(BiConsumer<String, TxEntity> action) {
    entities.forEach(action);
  }

  public void forEachReverse(BiConsumer<String, TxEntity> action) {
    ListIterator<Map.Entry<String, TxEntity>> iterator = new ArrayList<>(entities.entrySet()).listIterator(entities.size());
    while (iterator.hasPrevious()) {
      Map.Entry<String, TxEntity> entry = iterator.previous();
      action.accept(entry.getKey(),entry.getValue());
    }
  }

  public TxEntity get(String localTxId) {
    return entities.get(localTxId);
  }

  public boolean exists(String localTxId) {
    return entities.containsKey(localTxId);
  }

  public TxEntity put(String localTxId, TxEntity txEntity) {
    return entities.put(localTxId, txEntity);
  }

  public int size() {
    return entities.size();
  }

  public boolean hasCommittedTx() {
    return entities.entrySet().stream()
        .filter(map -> map.getValue().getState() == TxState.COMMITTED)
        .count() > 0;
  }

  public boolean hasCompensationSentTx() {
    return entities.entrySet().stream()
        .filter(map -> map.getValue().getState() == TxState.COMPENSATION_SENT)
        .count() > 0;
  }

  public boolean hasCompensationFailedTx() {
    return entities.entrySet().stream()
        .filter(map -> map.getValue().getState() == TxState.COMPENSATED_FAILED
            && map.getValue().getReverseRetries() > 0
            && map.getValue().getReverseRetries() > map.getValue().getRetriesCounter().get())
        .count() > 0;
  }
}