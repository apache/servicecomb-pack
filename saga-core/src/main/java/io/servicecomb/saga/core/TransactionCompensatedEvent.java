/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.core;

import java.util.Map;
import java.util.Set;

public class TransactionCompensatedEvent extends SagaEvent {

  private final SagaResponse response;

  TransactionCompensatedEvent(String sagaId, SagaRequest request) {
    this(sagaId, request, SagaResponse.EMPTY_RESPONSE);
  }

  public TransactionCompensatedEvent(String sagaId, SagaRequest request, SagaResponse response) {
    super(sagaId, request);
    this.response = response;
  }

  @Override
  public void gatherTo(
      Map<String, SagaRequest> hangingTransactions,
      Set<String> abortedTransactions,
      Map<String, SagaResponse> completedTransactions,
      Map<String, SagaResponse> completedCompensations) {

    completedCompensations.put(payload().id(), response);
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
