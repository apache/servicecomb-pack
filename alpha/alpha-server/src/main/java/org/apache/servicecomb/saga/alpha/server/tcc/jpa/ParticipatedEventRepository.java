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

package org.apache.servicecomb.saga.alpha.server.tcc.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ParticipatedEventRepository extends CrudRepository<ParticipatedEvent, Long> {

  @Query(value = "SELECT t FROM ParticipatedEvent AS t WHERE t.globalTxId = ?1")
  Optional<List<ParticipatedEvent>> findByGlobalTxId(String globalTxId);

  @Query(value = "SELECT t FROM ParticipatedEvent AS t WHERE t.globalTxId = ?1 and t.localTxId = ?2")
  Optional<ParticipatedEvent> findByUniqueKey(String globalTxId, String localTxId);
}
