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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.TccTxType;
import org.springframework.data.domain.Pageable;

public interface TccTxEventRepository {

  void saveGlobalTxEvent(GlobalTxEvent event);

  void saveParticipatedEvent(ParticipatedEvent event);

  void updateParticipatedEventStatus(ParticipatedEvent event);

  void coordinated(TccTxEvent event);

  void save(TccTxEvent event);

  Optional<List<TccTxEvent>> findByGlobalTxId(String globalTxId);

  Optional<List<ParticipatedEvent>> findParticipatedByGlobalTxId(String globalTxId);

  Optional<List<TccTxEvent>> findByGlobalTxIdAndTxType(String globalTxId, TccTxType tccTxType);

  Optional<TccTxEvent> findByUniqueKey(String globalTxId, String localTxId, TccTxType tccTxType);

  Optional<List<GlobalTxEvent>> findTimeoutGlobalTx(Date deadLine, String txType, Pageable pageable);

  void clearCompletedGlobalTx(Pageable pageable);

  Iterable<TccTxEvent> findAll();

  void deleteAll();

}
