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

  @Query(value = "SELECT c FROM Command AS c "
      + " WHERE c.globalTxId = :globalTxId "
      + " AND c.status != 'DONE' ")
  List<Command> findUnfinishedCommandByGlobalTxId(@Param("globalTxId") String globalTxId);

  @Query(value = "SELECT c FROM Command AS c "
      + " WHERE c.status = 'NEW' GROUP BY c")
  List<Command> findNewCommands();

  @Query(value = "SELECT * FROM Command AS c WHERE c.status = 'PENDING' ", nativeQuery = true)
  List<Command> findPendingCommands();
}
