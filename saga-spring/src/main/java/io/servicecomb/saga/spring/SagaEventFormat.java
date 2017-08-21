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

import io.servicecomb.saga.core.SagaEndedEvent;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaStartedEvent;
import io.servicecomb.saga.core.TransactionEndedEvent;
import io.servicecomb.saga.core.TransactionStartedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class SagaEventFormat {
  private final Map<String, BiFunction<String, String, SagaEvent>> eventFactories = new HashMap<String, BiFunction<String, String, SagaEvent>>() {{
    put("SagaStartedEvent", (sagaId, json) -> sagaStartedEvent(sagaId, json));
    put("TransactionStartedEvent", (sagaId, json) -> transactionStartedEvent(sagaId, json));
    put("TransactionEndedEvent", (sagaId, json) -> transactionEndedEvent(sagaId, json));
    put("SagaEndedEvent", (sagaId, json) -> sagaEndedEvent(sagaId, json));
  }};

  SagaEventFormat() {
  }

  SagaEvent toSagaEvent(SagaEventEntity event) {
    return eventFactories.get(event.type()).apply(event.sagaId(), event.contentJson());
  }

  private SagaEvent sagaStartedEvent(String sagaId, String json) {
    return new SagaStartedEvent(sagaId, json, null);
  }

  private SagaEvent transactionStartedEvent(String sagaId, String json) {
    return new TransactionStartedEvent(sagaId, null);
  }

  private SagaEvent transactionEndedEvent(String sagaId, String json) {
    return new TransactionEndedEvent(sagaId, null, null);
  }

  private SagaEvent sagaEndedEvent(String sagaId, String json) {
    return new SagaEndedEvent(sagaId, null);
  }
}