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

package org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc;

import java.util.Date;
import java.util.Optional;
import javax.transaction.Transactional;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface MasterLockEntityRepository extends CrudRepository<MasterLock, String> {

  Optional<MasterLock> findMasterLockByServiceName(String serviceName);

  @Transactional
  @Modifying
  @Query(value = "INSERT INTO master_lock "
      + "(serviceName, expireTime, lockedTime, instanceId) "
      + "VALUES "
      + "(?1, ?2, ?3, ?4)", nativeQuery = true)
  int initLock(
      @Param("serviceName") String serviceName,
      @Param("expireTime") Date expireTime,
      @Param("lockedTime") Date lockedTime,
      @Param("instanceId") String instanceId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLock t "
      + "SET t.expireTime = :expireTime"
      + ",t.lockedTime = :lockedTime "
      + ",t.instanceId = :instanceId "
      + "WHERE t.serviceName = :serviceName AND (t.expireTime <= :lockedTime OR t.instanceId = :instanceId)")
  int updateLock(
      @Param("serviceName") String serviceName,
      @Param("lockedTime") Date lockedTime,
      @Param("expireTime") Date expireTime,
      @Param("instanceId") String instanceId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("UPDATE org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLock t "
      + "SET t.expireTime = :expireTime "
      + "WHERE t.serviceName = :serviceName")
  int unLock(@Param("serviceName") String serviceName,
      @Param("expireTime") Date expireTime);
}
