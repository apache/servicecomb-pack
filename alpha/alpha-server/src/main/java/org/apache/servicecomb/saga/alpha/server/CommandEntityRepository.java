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
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CommandEntityRepository extends CrudRepository<CommandEntity, Long> {
  Optional<CommandEntity> findByCommandGlobalTxIdAndCommandLocalTxId(String globalTxId, String localTxId);

  @Transactional
  @Modifying
  @Query("UPDATE org.apache.servicecomb.saga.alpha.server.CommandEntity c "
      + "SET c.command.status = :status "
      + "WHERE c.command.globalTxId = :globalTxId "
      + "AND c.command.localTxId = :localTxId")
  void updateStatusByGlobalTxIdAndLocalTxId(
      @Param("status") String status,
      @Param("globalTxId") String globalTxId,
      @Param("localTxId") String localTxId);

  List<CommandEntity> findByCommandGlobalTxIdAndCommandStatus(String globalTxId, String status);

  @Query("FROM CommandEntity c "
      + "WHERE id IN ("
      + " SELECT MAX(id) FROM CommandEntity c1 WHERE c1.command.status <> 'DONE' GROUP BY c1.command.globalTxId"
      + ") "
      + "ORDER BY c.id ASC")
  List<CommandEntity> findFirstGroupByCommandGlobalTxIdOrderByIdDesc(Pageable pageable);
}
