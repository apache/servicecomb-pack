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

package org.apache.servicecomb.pack.alpha.spec.saga.db;

import java.util.List;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import org.apache.servicecomb.pack.alpha.core.TxTimeout;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface TxTimeoutEntityRepository extends CrudRepository<TxTimeout, Long> {

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.pack.alpha.core.TxTimeout t "
      + "SET t.status = :status "
      + "WHERE t.globalTxId = :globalTxId "
      + "  AND t.localTxId = :localTxId")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("status") String status,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  @Lock(LockModeType.OPTIMISTIC)
  @Query("SELECT t FROM TxTimeout AS t "
      + "WHERE t.status = 'NEW' "
      + "  AND t.expiryTime < CURRENT_TIMESTAMP "
      + "ORDER BY t.expiryTime ASC")
  List<TxTimeout> findFirstTimeoutTxOrderByExpireTimeAsc(Pageable pageable);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE TxTimeout t "
      + "SET t.status = 'DONE' "
      + "WHERE t.status != 'DONE' AND EXISTS ("
      + "  SELECT t1.globalTxId FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type != t.type"
      + ")")
  void updateStatusOfFinishedTx();
}
