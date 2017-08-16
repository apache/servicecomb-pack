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

package io.servicecomb.saga.core.application.interpreter;

import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.RequestProcessTask;
import io.servicecomb.saga.core.SagaEndTask;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaStartTask;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.Transport;

public class SagaTaskFactory {

  private final EventStore eventStore;
  private final Transport transport;

  public SagaTaskFactory(EventStore eventStore, Transport transport) {
    this.eventStore = eventStore;
    this.transport = transport;
  }

  SagaTask newStartTask(long sagaId, String requestJson) {
    return new SagaStartTask(sagaId, requestJson, eventStore);
  }

  SagaTask newEndTask(long sagaId) {
    return new SagaEndTask(sagaId, eventStore);
  }

  SagaTask newRequestTask(long sagaId, SagaRequest sagaRequest) {
    return new RequestProcessTask(sagaId, sagaRequest, eventStore, transport);
  }
}