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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.servicecomb.saga.core.CompensationEndedEvent;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class SagaEventFormat {
  private final Map<String, BiFunction<String, String, SagaEvent>> eventFactories = new HashMap<String, BiFunction<String, String, SagaEvent>>() {{
    put(SagaStartedEvent.class.getSimpleName(), (sagaId, json) -> sagaStartedEvent(sagaId, json));
    put(TransactionStartedEvent.class.getSimpleName(), (sagaId, json) -> transactionStartedEvent(sagaId, json));
    put(TransactionEndedEvent.class.getSimpleName(), (sagaId, json) -> transactionEndedEvent(sagaId, json));
    put(TransactionAbortedEvent.class.getSimpleName(), (sagaId, json) -> transactionAbortedEvent(sagaId, json));
    put(CompensationStartedEvent.class.getSimpleName(), (sagaId, json) -> compensationStartedEvent(sagaId, json));
    put(CompensationEndedEvent.class.getSimpleName(), (sagaId, json) -> compensationEndedEvent(sagaId, json));
    put(SagaEndedEvent.class.getSimpleName(), (sagaId, json) -> sagaEndedEvent(sagaId));
  }};

  private final ObjectMapper objectMapper = new ObjectMapper();

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
      return new TransactionEndedEvent(sagaId, sagaRequestResponse.request(), sagaRequestResponse.response());
    } catch (IOException e) {
      throw new SagaException("Failed to deserialize transaction: sage Id: " + sagaId + " json: " + json, e);
    }
  }

  private SagaEvent transactionAbortedEvent(String sagaId, String json) {
    try {
      SagaRequestException sagaRequestException = objectMapper.readValue(json, SagaRequestException.class);
      return new TransactionAbortedEvent(sagaId, sagaRequestException.request(), sagaRequestException.response());
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

  private SagaEvent compensationEndedEvent(String sagaId, String json) {
    try {
      SagaRequestResponse requestResponse = objectMapper.readValue(json, SagaRequestResponse.class);
      return new CompensationEndedEvent(sagaId, requestResponse.request(), requestResponse.response());
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