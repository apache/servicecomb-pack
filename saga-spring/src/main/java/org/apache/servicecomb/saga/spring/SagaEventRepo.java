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

package org.apache.servicecomb.saga.spring;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

interface SagaEventRepo extends PagingAndSortingRepository<SagaEventEntity, Long> {

  // TODO: 8/21/2017 replace with hql?
  @Query(value = "SELECT * FROM saga_event_entity "
      + "WHERE saga_id NOT IN ("
      + "  SELECT DISTINCT saga_id FROM saga_event_entity"
      + "  WHERE type = 'SagaEndedEvent'"
      + ")", nativeQuery = true)
  List<SagaEventEntity> findIncompleteSagaEventsGroupBySagaId();

  Page<SagaEventEntity> findByTypeAndCreationTimeBetweenOrderByIdDesc(String type, Date startTime, Date endTime,
      Pageable pageable);

  SagaEventEntity findFirstByTypeAndSagaId(String type, String sagaId);

  List<SagaEventEntity> findBySagaId(String sagaId);
}
