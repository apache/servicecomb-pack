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

package org.apache.servicecomb.saga.alpha.core;

import java.util.List;
import java.util.Optional;
import org.apache.servicecomb.saga.common.EventType;

/**
 * Repository for {@link TxEvent}
 */
public interface TxEventRepository {

  /**
   * Save a {@link TxEvent}.
   *
   * @param event
   */
  void save(TxEvent event);

  /**
   * Find a {@link TxEvent} which satisfies below requirements:
   *
   * <ol>
   *   <li>{@link TxEvent#type} is {@link EventType#TxAbortedEvent}</li>
   *   <li>There are no {@link TxEvent} which has the same {@link TxEvent#globalTxId} and {@link TxEvent#type} is {@link EventType#TxEndedEvent} or {@link EventType#SagaEndedEvent}</li>
   * </ol>
   * @return
   */
  Optional<TxEvent> findFirstAbortedGlobalTransaction();

  /**
   * Find timeout {@link TxEvent}s. A timeout TxEvent satisfies below requirements:
   *
   * <ol>
   *  <li>{@link TxEvent#type} is {@link EventType#TxStartedEvent} or {@link EventType#SagaStartedEvent}</li>
   *  <li>Current time greater than {@link TxEvent#expiryTime}</li>
   *  <li>There are no corresponding {@link TxEvent} which type is <code>TxEndedEvent</code> or <code>SagaEndedEvent</code></li>
   * </ol>
   *
   * @return
   */
  List<TxEvent> findTimeoutEvents();

  /**
   * Find a {@link TxEvent} which satisfies below requirements:
   * <ol>
   *   <li>{@link TxEvent#type} is {@link EventType#TxStartedEvent}</li>
   *   <li>{@link TxEvent#globalTxId} equals to param <code>globalTxId</code></li>
   *   <li>{@link TxEvent#localTxId} equals to param <code>localTxId</code></li>
   * </ol>
   *
   * @param globalTxId
   * @param localTxId
   * @return {@link TxEvent}
   */
  Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId);

  /**
   * Find {@link TxEvent}s which satisfy below requirements:
   * <ol>
   *   <li>{@link TxEvent#globalTxId} equals to param <code>globalTxId</code></li>
   *   <li>{@link TxEvent#type} equals to param <code>type</code></li>
   * </ol>
   *
   * @param globalTxId globalTxId to search for
   * @param type       event type to search for
   * @return
   */
  List<TxEvent> findTransactions(String globalTxId, String type);

  /**
   * Find a {@link TxEvent} which satisfies below requirements:
   * <ol>
   *   <li>{@link TxEvent#type} equals to {@link EventType#TxEndedEvent}</li>
   *   <li>{@link TxEvent#surrogateId} greater than param <code>id</code></li>
   *   <li>{@link TxEvent#type} equals to param <code>type</code></li>
   *   <li>There is a corresponding <code>TxAbortedEvent</code></li>
   *   <li>There is no coresponding <code>TxCompensatedEvent</code></li>
   * </ol>
   *
   * @param id
   * @return
   */
  List<TxEvent> findFirstUncompensatedEventByIdGreaterThan(long id, String type);

  /**
   * Find a {@link TxEvent} which satisfies below requirements:
   *
   * <ol>
   *   <li>{@link TxEvent#type} equals to {@link EventType#TxCompensatedEvent}</li>
   *   <li>{@link TxEvent#surrogateId} greater than param <code>id</code></li>
   * </ol>
   *
   * @param id
   * @return
   */
  Optional<TxEvent> findFirstCompensatedEventByIdGreaterThan(long id);

  /**
   * Delete duplicated {@link TxEvent}s which {@link TxEvent#type} equals param <code>type</code>.
   *
   * @param type event type
   */
  void deleteDuplicateEvents(String type);
}
