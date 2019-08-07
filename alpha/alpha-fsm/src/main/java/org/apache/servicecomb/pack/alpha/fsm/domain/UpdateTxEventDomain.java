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

package org.apache.servicecomb.pack.alpha.fsm.domain;

import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensatedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;

public class UpdateTxEventDomain implements DomainEvent {
  private String parentTxId;
  private String localTxId;
  private TxState state;
  private byte[] throwablePayLoads;
  private BaseEvent event;

  public UpdateTxEventDomain(TxEndedEvent event) {
    this.event = event;
    this.parentTxId = event.getParentTxId();
    this.localTxId = event.getLocalTxId();
    this.state = TxState.COMMITTED;
  }

  public UpdateTxEventDomain(TxAbortedEvent event) {
    this.event = event;
    this.parentTxId = event.getParentTxId();
    this.localTxId = event.getLocalTxId();
    this.throwablePayLoads = event.getPayloads();
    this.state = TxState.FAILED;
  }

  public UpdateTxEventDomain(TxCompensatedEvent event) {
    this.event = event;
    this.parentTxId = event.getParentTxId();
    this.localTxId = event.getLocalTxId();
    this.state = TxState.COMPENSATED;
  }


  public String getParentTxId() {
    return parentTxId;
  }

  public void setParentTxId(String parentTxId) {
    this.parentTxId = parentTxId;
  }

  public String getLocalTxId() {
    return localTxId;
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId = localTxId;
  }

  public TxState getState() {
    return state;
  }

  public void setState(TxState state) {
    this.state = state;
  }

  public byte[] getThrowablePayLoads() {
    return throwablePayLoads;
  }

  public void setThrowablePayLoads(byte[] throwablePayLoads) {
    this.throwablePayLoads = throwablePayLoads;
  }

  @Override
  public BaseEvent getEvent() {
    return event;
  }
}
