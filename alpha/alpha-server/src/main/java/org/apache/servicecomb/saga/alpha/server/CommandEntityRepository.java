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

package org.apache.servicecomb.saga.alpha.server;

import java.util.List;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CommandEntityRepository extends CrudRepository<Command, Long> {

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.saga.alpha.core.Command c "
      + "SET c.status = :toStatus "
      + "WHERE c.globalTxId = :globalTxId "
      + "  AND c.localTxId = :localTxId "
      + "  AND c.status = :fromStatus")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.saga.alpha.core.Command c "
      + "SET c.status = :status "
      + "WHERE c.globalTxId = :globalTxId "
      + "  AND c.localTxId = :localTxId")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("status") String status,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  List<Command> findByGlobalTxIdAndStatus(String globalTxId, String status);

  // TODO: 2018/1/18 we assumed compensation will never fail. if all service instances are not reachable, we have to set up retry mechanism for pending commands
  @Lock(LockModeType.OPTIMISTIC)
  @Query(value = "SELECT * FROM Command AS c "
      + "WHERE c.eventId IN ("
      + " SELECT MAX(c1.eventId) FROM Command AS c1 "
      + " INNER JOIN Command AS c2 on c1.globalTxId = c2.globalTxId"
      + " WHERE c1.status = 'NEW' "
      + " GROUP BY c1.globalTxId "
      + " HAVING MAX( CASE c2.status WHEN 'PENDING' THEN 1 ELSE 0 END ) = 0) "
      + "ORDER BY c.eventId ASC LIMIT 1", nativeQuery = true)
  List<Command> findFirstGroupByGlobalTxIdWithoutPendingOrderByIdDesc();
}
