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

package io.servicecomb.saga.format;

import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_END_REQUEST;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.servicecomb.saga.core.FailedSagaRequestContext;
import io.servicecomb.saga.core.SagaEndedEvent;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaStartedEvent;
import io.servicecomb.saga.core.SuccessfulSagaRequestContext;
import io.servicecomb.saga.core.TransactionAbortedEvent;
import io.servicecomb.saga.core.TransactionCompensatedEvent;
import io.servicecomb.saga.core.TransactionEndedEvent;
import io.servicecomb.saga.core.TransactionStartedEvent;
import io.servicecomb.saga.transports.TransportFactory;

public class JacksonSagaEventFormat implements SagaEventFormat {
  private final Map<String, BiFunction<String, String, SagaEvent>> eventFactories = new HashMap<String, BiFunction<String, String, SagaEvent>>() {{
    put(SagaStartedEvent.class.getSimpleName(), (sagaId, json) -> sagaStartedEvent(sagaId, json));
    put(TransactionStartedEvent.class.getSimpleName(), (sagaId, json) -> transactionStartedEvent(sagaId, json));
    put(TransactionEndedEvent.class.getSimpleName(), (sagaId, json) -> transactionEndedEvent(sagaId, json));
    put(TransactionAbortedEvent.class.getSimpleName(), (sagaId, json) -> transactionAbortedEvent(sagaId, json));
    put(TransactionCompensatedEvent.class.getSimpleName(), (sagaId, json) -> compensationEndedEvent(sagaId, json));
    put(SagaEndedEvent.class.getSimpleName(), (sagaId, json) -> sagaEndedEvent(sagaId));
  }};

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final TransportFactory transportFactory;

  public JacksonSagaEventFormat(TransportFactory transportFactory) {
    this.transportFactory = transportFactory;
  }

  @Override
  public SagaEvent toSagaEvent(String sagaId, String eventType, String contentJson) {
    return eventFactories.get(eventType).apply(sagaId, contentJson);
  }

  private SagaEvent sagaStartedEvent(String sagaId, String json) {
    return new SagaStartedEvent(
        sagaId,
        json,
        SAGA_START_REQUEST);
  }

  private SagaEvent transactionStartedEvent(String sagaId, String json) {
    try {
      return new TransactionStartedEvent(sagaId, objectMapper.readValue(json, JsonSagaRequest.class).with(transportFactory));
    } catch (IOException e) {
      throw new SagaException(cause(sagaId, json), e);
    }
  }

  private SagaEvent transactionEndedEvent(String sagaId, String json) {
    try {
      SuccessfulSagaRequestContext context = objectMapper.readValue(json, SuccessfulSagaRequestContext.class);
      return new TransactionEndedEvent(sagaId, context.request().with(transportFactory), context.response());
    } catch (IOException e) {
      throw new SagaException(cause(sagaId, json), e);
    }
  }

  private SagaEvent transactionAbortedEvent(String sagaId, String json) {
    try {
      FailedSagaRequestContext context = objectMapper.readValue(json, FailedSagaRequestContext.class);
      return new TransactionAbortedEvent(sagaId, context.request().with(transportFactory), context.response());
    } catch (IOException e) {
      throw new SagaException(cause(sagaId, json), e);
    }
  }

  private SagaEvent compensationEndedEvent(String sagaId, String json) {
    try {
      SuccessfulSagaRequestContext context = objectMapper.readValue(json, SuccessfulSagaRequestContext.class);
      return new TransactionCompensatedEvent(sagaId, context.request().with(transportFactory), context.response());
    } catch (IOException e) {
      throw new SagaException(cause(sagaId, json), e);
    }
  }

  private SagaEvent sagaEndedEvent(String sagaId) {
    return new SagaEndedEvent(
        sagaId,
        SAGA_END_REQUEST);
  }

  private String cause(String sagaId, String json) {
    return "Failed to deserialize saga event of sage id: " + sagaId + " from json: " + json;
  }
}
