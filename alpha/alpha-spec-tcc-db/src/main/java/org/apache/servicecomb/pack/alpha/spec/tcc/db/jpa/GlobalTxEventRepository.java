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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface GlobalTxEventRepository extends CrudRepository<GlobalTxEvent, Long> {

  @Query(value = "SELECT t FROM GlobalTxEvent AS t WHERE t.globalTxId = ?1")
  Optional<List<GlobalTxEvent>> findByGlobalTxId(String globalTxId);

  @Query(value = "SELECT t FROM GlobalTxEvent AS t WHERE t.globalTxId = ?1 and t.localTxId = ?2 and t.txType = ?3")
  Optional<GlobalTxEvent> findByUniqueKey(String globalTxId, String localTxId, String txType);

  @Query(value = "SELECT t FROM GlobalTxEvent AS t WHERE t.creationTime < ?1 and t.txType = ?2 order by t.creationTime asc")
  Optional<List<GlobalTxEvent>> findTimeoutGlobalTx(Date deadLine, String txType, Pageable pageable);

  @Query(value = "SELECT t.globalTxId from GlobalTxEvent as t GROUP BY t.globalTxId HAVING COUNT(t.globalTxId) = 2 "
      + "AND NOT EXISTS (select 1 from  ParticipatedEvent as b where b.globalTxId = t.globalTxId)"
  )
  Optional<List<String>> findCompletedGlobalTx(Pageable pageable);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query(value = "DELETE FROM GlobalTxEvent as t where t.globalTxId in (?1)")
  void deleteByGlobalId(String globalTxId);

}
