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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface TccTxEventDBRepository extends CrudRepository<TccTxEvent, Long> {

  @Query(value = "SELECT t FROM TccTxEvent AS t WHERE t.globalTxId = ?1")
  Optional<List<TccTxEvent>> findByGlobalTxId(String globalTxId);

  @Query(value = "SELECT t FROM TccTxEvent AS t WHERE t.globalTxId = ?1 and t.localTxId = ?2 and t.txType = ?3")
  Optional<TccTxEvent> findByUniqueKey(String globalTxId, String localTxId, String txType);

  @Query(value = "SELECT t FROM TccTxEvent AS t WHERE t.globalTxId = ?1 and t.txType = ?2")
  Optional<List<TccTxEvent>> findByGlobalTxIdAndTxType(String globalTxId, String txType);
}
