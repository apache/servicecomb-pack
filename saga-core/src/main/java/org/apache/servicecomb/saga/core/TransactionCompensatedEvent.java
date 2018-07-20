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

package org.apache.servicecomb.saga.core;

public class TransactionCompensatedEvent extends SagaEvent {

  private final SagaResponse response;

  public TransactionCompensatedEvent(String sagaId, SagaRequest request) {
    this(sagaId, request, SagaResponse.EMPTY_RESPONSE);
  }

  public TransactionCompensatedEvent(String sagaId, SagaRequest request, SagaResponse response) {
    super(sagaId, request);
    this.response = response;
  }

  @Override
  public void gatherTo(EventContext sagaContext) {
    sagaContext.compensateTransaction(payload(), response);
  }

  @Override
  public String json(ToJsonFormat toJsonFormat) {
    return toJsonFormat.toJson(payload(), response);
  }

  @Override
  public String toString() {
    return "TransactionCompensatedEvent{id="
        + payload().id()
        + ", sagaId=" + sagaId
        + ", operation="
        + payload().compensation()
        + "}";
  }

  public SagaResponse response() {
    return response;
  }
}
