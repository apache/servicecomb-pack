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
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;

public class AddTxEventDomain implements DomainEvent {
  private TxState state = TxState.ACTIVE;
  private int reverseRetries;
  private int reverseTimeout;
  private int retryDelayInMilliseconds;
  private String compensationMethod;
  private byte[] payloads;
  private BaseEvent event;

  public AddTxEventDomain(TxStartedEvent event) {
    this.event = event;
    this.compensationMethod = event.getCompensationMethod();
    this.payloads = event.getPayloads();
    this.reverseRetries = event.getReverseRetries();
    this.reverseTimeout = event.getReverseTimeout();
    this.retryDelayInMilliseconds = event.getRetryDelayInMilliseconds();
  }

  public TxState getState() {
    return state;
  }

  public String getCompensationMethod() {
    return compensationMethod;
  }

  public void setCompensationMethod(String compensationMethod) {
    this.compensationMethod = compensationMethod;
  }

  public int getReverseRetries() {
    return reverseRetries;
  }

  public void setReverseRetries(int reverseRetries) {
    this.reverseRetries = reverseRetries;
  }

  public int getReverseTimeout() {
    return reverseTimeout;
  }

  public void setReverseTimeout(int reverseTimeout) {
    this.reverseTimeout = reverseTimeout;
  }

  public int getRetryDelayInMilliseconds() {
    return retryDelayInMilliseconds;
  }

  public void setRetryDelayInMilliseconds(int retryDelayInMilliseconds) {
    this.retryDelayInMilliseconds = retryDelayInMilliseconds;
  }

  public byte[] getPayloads() {
    return payloads;
  }

  public void setPayloads(byte[] payloads) {
    this.payloads = payloads;
  }

  @Override
  public BaseEvent getEvent() {
    return event;
  }
}
