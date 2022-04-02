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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.domain;

import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckFailedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckSucceedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.internal.CompensateAckTimeoutEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.internal.ComponsitedCheckEvent;

public class UpdateTxEventDomain implements DomainEvent {
  private String localTxId;
  private TxState state;
  private byte[] throwablePayLoads;
  private BaseEvent event;

  public UpdateTxEventDomain(TxEndedEvent event) {
    this.event = event;
    this.localTxId = event.getLocalTxId();
    this.state = TxState.COMMITTED;
  }

  public UpdateTxEventDomain(TxAbortedEvent event) {
    this.event = event;
    this.localTxId = event.getLocalTxId();
    this.throwablePayLoads = event.getPayloads();
    this.state = TxState.FAILED;
  }

  public UpdateTxEventDomain(TxCompensateAckSucceedEvent event) {
    this.event = event;
    this.localTxId = event.getLocalTxId();
    this.state = TxState.COMPENSATED_SUCCEED;
  }

  public UpdateTxEventDomain(TxCompensateAckFailedEvent event) {
    this.event = event;
    this.localTxId = event.getLocalTxId();
    this.state = TxState.COMPENSATED_FAILED;
  }

  public UpdateTxEventDomain(CompensateAckTimeoutEvent event) {
    this.event = event;
    this.localTxId = event.getLocalTxId();
    this.throwablePayLoads = event.getPayloads();
    this.state = TxState.COMPENSATED_FAILED;
  }

  public UpdateTxEventDomain(ComponsitedCheckEvent event) {
    this.event = event;
    this.localTxId = event.getLocalTxId();
    this.state = event.getPreComponsitedState();
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public TxState getState() {
    return state;
  }

  public byte[] getThrowablePayLoads() {
    return throwablePayLoads;
  }

  @Override
  public BaseEvent getEvent() {
    return event;
  }
}
