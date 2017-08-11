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

import static io.servicecomb.saga.core.Compensation.SAGA_END_COMPENSATION;
import static io.servicecomb.saga.core.Compensation.SAGA_START_COMPENSATION;
import static io.servicecomb.saga.core.Transaction.SAGA_END_TRANSACTION;
import static io.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;

import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.TaskAwareSagaRequest;

public class SagaTaskFactory {

  private final SagaTask sagaStartTask;
  private final SagaTask sagaRequestTask;
  private final SagaTask sagaEndTask;

  public SagaTaskFactory(SagaTask sagaStartTask, SagaTask sagaRequestTask, SagaTask sagaEndTask) {
    this.sagaStartTask = sagaStartTask;
    this.sagaRequestTask = sagaRequestTask;
    this.sagaEndTask = sagaEndTask;
  }

  TaskAwareSagaRequest newStartTask(String requestJson) {
    return new TaskAwareSagaRequest("saga-start", SAGA_START_TRANSACTION, SAGA_START_COMPENSATION, sagaStartTask, requestJson);
  }

  TaskAwareSagaRequest newEndTask(String requestJson) {
    return new TaskAwareSagaRequest("saga-end", SAGA_END_TRANSACTION, SAGA_END_COMPENSATION, sagaEndTask, requestJson);
  }

  TaskAwareSagaRequest newRequestTask(SagaRequest sagaRequest, String requests) {
    return new TaskAwareSagaRequest(sagaRequest, sagaRequestTask, requests);
  }
}