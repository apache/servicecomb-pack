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

package org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.jdbc.jpa;

import org.apache.servicecomb.pack.alpha.core.Election;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.Date;

interface ElectionEntityRepository extends CrudRepository<Election, String> {

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.pack.alpha.core.Election t "
      + "SET t.lock_until = :lock_until"
      + ",t.locked_at = :now "
      + ",t.locked_by = :locked_by "
      + "WHERE t.name = :name "
      + "  AND (t.lock_until <= :now OR t.locked_by = :hostname)")
  boolean vote(
          @Param("name") String name,
          @Param("now") Date now,
          @Param("lock_until") Date lock_until,
          @Param("locked_by") String locked_by);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.pack.alpha.core.Election t "
          + "SET t.lock_until = :lock_until "
          + "WHERE t.name = :name")
  void abstain(
          @Param("name") String name,
          @Param("lock_until") Date lock_until);
}
