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

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

interface TxEventEnvelopeRepository extends CrudRepository<TxEvent, Long> {
  List<TxEvent> findByGlobalTxId(String globalTxId);

  @Query("SELECT DISTINCT new org.apache.servicecomb.saga.alpha.core.TxEvent("
      + "t.serviceName, t.instanceId, t.globalTxId, t.localTxId, t.parentTxId, t.type, t.compensationMethod, t.payloads"
      + ") FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 AND t.type = ?2")
  List<TxEvent> findByEventGlobalTxIdAndEventType(String globalTxId, String type);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 AND t.type = 'TxStartedEvent' AND EXISTS ( "
      + "  SELECT t1.globalTxId"
      + "  FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = ?1 "
      + "  AND t1.localTxId = t.localTxId "
      + "  AND t1.type = 'TxEndedEvent'"
      + ") AND NOT EXISTS ( "
      + "  SELECT t2.globalTxId"
      + "  FROM TxEvent t2 "
      + "  WHERE t2.globalTxId = ?1 "
      + "  AND t2.localTxId = t.localTxId "
      + "  AND t2.type = 'TxCompensatedEvent') "
      + "ORDER BY t.surrogateId ASC")
  List<TxEvent> findStartedEventEnvelopesWithMatchingEndedButNotCompensatedEvents(String globalTxId);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type = 'TxEndedEvent' AND t.surrogateId > ?2 AND EXISTS ( "
      + "  SELECT t1.globalTxId"
      + "  FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "  AND t1.type = 'TxAbortedEvent'"
      + ") AND NOT EXISTS ( "
      + "  SELECT t2.globalTxId"
      + "  FROM TxEvent t2 "
      + "  WHERE t2.globalTxId = ?1 "
      + "  AND t2.localTxId = t.localTxId "
      + "  AND t2.type = 'TxCompensatedEvent') "
      + "ORDER BY t.surrogateId ASC")
  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId, Pageable pageable);

  Optional<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId);
}
