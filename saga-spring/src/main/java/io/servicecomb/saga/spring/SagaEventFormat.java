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

package io.servicecomb.saga.spring;

import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_END_REQUEST;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.servicecomb.saga.core.CompensationStartedEvent;
import io.servicecomb.saga.core.SagaEndedEvent;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaRequestException;
import io.servicecomb.saga.core.SagaRequestResponse;
import io.servicecomb.saga.core.SagaStartedEvent;
import io.servicecomb.saga.core.TransactionAbortedEvent;
import io.servicecomb.saga.core.TransactionEndedEvent;
import io.servicecomb.saga.core.TransactionStartedEvent;
import io.servicecomb.saga.core.application.interpreter.JsonSagaRequest;

class SagaEventFormat {
  private final Map<String, BiFunction<String, String, SagaEvent>> eventFactories = new HashMap<String, BiFunction<String, String, SagaEvent>>() {{
    put("SagaStartedEvent", (sagaId, json) -> sagaStartedEvent(sagaId, json));
    put("TransactionStartedEvent", (sagaId, json) -> transactionStartedEvent(sagaId, json));
    put("TransactionEndedEvent", (sagaId, json) -> transactionEndedEvent(sagaId, json));
    put("TransactionAbortedEvent", (sagaId, json) -> transactionAbortedEvent(sagaId, json));
    put("CompensationStartedEvent", (sagaId, json) -> compensationStartedEvent(sagaId, json));
    put("SagaEndedEvent", (sagaId, json) -> sagaEndedEvent(sagaId));
  }};
  
  private ObjectMapper objectMapper=new ObjectMapper();

  SagaEventFormat() {
  }

  SagaEvent toSagaEvent(SagaEventEntity event) {
    return eventFactories.get(event.type()).apply(event.sagaId(), event.contentJson());
  }

  private SagaEvent sagaStartedEvent(String sagaId, String json) {
    return new SagaStartedEvent(
        sagaId,
        json,
        SAGA_START_REQUEST);
  }

  private SagaEvent transactionStartedEvent(String sagaId, String json) {
    try {
      return new TransactionStartedEvent(sagaId, objectMapper.readValue(json, JsonSagaRequest.class));
    } catch (IOException e) {
      throw new SagaException("Failed to deserialize transaction: sage Id: " + sagaId + " json: " + json, e);
    }
  }

  private SagaEvent transactionEndedEvent(String sagaId, String json) {
    try {
      SagaRequestResponse sagaRequestResponse = objectMapper.readValue(json, SagaRequestResponse.class);
      return new TransactionEndedEvent(sagaId, sagaRequestResponse.sagaRequest(), sagaRequestResponse.sagaResponse());
    } catch (IOException e) {
      throw new SagaException("Failed to deserialize transaction: sage Id: " + sagaId + " json: " + json, e);
    }
  }

  private SagaEvent transactionAbortedEvent(String sagaId, String json) {
    try {
      SagaRequestException sagaRequestException = objectMapper.readValue(json, SagaRequestException.class);
      return new TransactionAbortedEvent(sagaId, sagaRequestException.sagaRequest(), sagaRequestException.exception());
    } catch (IOException e) {
      throw new SagaException("Failed to deserialize transaction: sage Id: " + sagaId + " json: " + json, e);
    }
  }
  
  private SagaEvent compensationStartedEvent(String sagaId, String json) {
    try {
      return new CompensationStartedEvent(sagaId, objectMapper.readValue(json, JsonSagaRequest.class));
    } catch (IOException e) {
      throw new SagaException("Failed to deserialize transaction: sage Id: " + sagaId + " json: " + json, e);
    }
  }
  
  private SagaEvent sagaEndedEvent(String sagaId) {
    return new SagaEndedEvent(
        sagaId,
        SAGA_END_REQUEST);
  }
}