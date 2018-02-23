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

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

interface TxEventEnvelopeRepository extends CrudRepository<TxEvent, Long> {
  List<TxEvent> findByGlobalTxId(String globalTxId);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type = 'TxAbortedEvent' AND NOT EXISTS( "
      + "  SELECT t1.globalTxId FROM TxEvent t1"
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.type IN ('TxEndedEvent', 'SagaEndedEvent')) AND NOT EXISTS ( "
      + "  SELECT t3.globalTxId FROM TxEvent t3 "
      + "  WHERE t3.globalTxId = t.globalTxId "
      + "    AND t3.localTxId = t.localTxId "
      + "    AND t3.surrogateId != t.surrogateId "
      + "    AND t3.creationTime > t.creationTime) AND (("
      + "SELECT MIN(t2.retries) FROM TxEvent t2 "
      + "WHERE t2.globalTxId = t.globalTxId "
      + "  AND t2.localTxId = t.localTxId "
      + "  AND t2.type = 'TxStartedEvent') = 0 "
      + "OR t.globalTxId = t.localTxId)")
  Optional<TxEvent> findFirstAbortedGlobalTxByType();

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type IN ('TxStartedEvent', 'SagaStartedEvent') "
      + "  AND t.expiryTime < CURRENT_TIMESTAMP AND NOT EXISTS( "
      + "  SELECT t1.globalTxId FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type != t.type"
      + ")")
  List<TxEvent> findTimeoutEvents(Pageable pageable);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 "
      + "  AND t.localTxId = ?2 "
      + "  AND t.type = 'TxStartedEvent'")
  Optional<TxEvent> findFirstStartedEventByGlobalTxIdAndLocalTxId(String globalTxId, String localTxId);

  @Query("SELECT DISTINCT new org.apache.servicecomb.saga.alpha.core.TxEvent("
      + "t.serviceName, t.instanceId, t.globalTxId, t.localTxId, t.parentTxId, "
      + "t.type, t.compensationMethod, t.payloads "
      + ") FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 AND t.type = ?2 "
      + "  AND ( SELECT MIN(t1.retries) FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type = 'TxStartedEvent' ) = 0 ")
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
  List<TxEvent> findStartedEventsWithMatchingEndedButNotCompensatedEvents(String globalTxId);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type = ?1 AND t.surrogateId > ?2 AND EXISTS ( "
      + "  SELECT t1.globalTxId FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.type = 'TxAbortedEvent' AND NOT EXISTS ( "
      + "    SELECT t2.globalTxId FROM TxEvent t2 "
      + "    WHERE t2.globalTxId = t1.globalTxId "
      + "      AND t2.localTxId = t1.localTxId "
      + "      AND t2.type = 'TxStartedEvent' "
      + "      AND t2.creationTime > t1.creationTime)) AND NOT EXISTS ( "
      + "  SELECT t3.globalTxId FROM TxEvent t3 "
      + "  WHERE t3.globalTxId = t.globalTxId "
      + "    AND t3.localTxId = t.localTxId "
      + "    AND t3.type = 'TxCompensatedEvent') AND ( "
      + "  SELECT MIN(t4.retries) FROM TxEvent t4 "
      + "  WHERE t4.globalTxId = t.globalTxId "
      + "    AND t4.localTxId = t.localTxId "
      + "    AND t4.type = 'TxStartedEvent' ) = 0 "
      + "ORDER BY t.surrogateId ASC")
  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId, Pageable pageable);

  Optional<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM TxEvent t "
      + "WHERE t.type = ?1 AND t.surrogateId NOT IN ("
      + " SELECT MAX(t1.surrogateId) FROM TxEvent t1 "
      + " WHERE t1.type = ?1 "
      + " GROUP BY t1.globalTxId"
      + ")")
  void deleteByType(String type);
}
