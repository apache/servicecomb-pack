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

import static io.servicecomb.saga.core.Compensation.SAGA_START_COMPENSATION;
import static io.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;

public class SagaStartTask implements SagaTask {

  private final String sagaId;
  private final String requestJson;
  private final SagaLog sagaLog;

  public SagaStartTask(String sagaId, String requestJson, SagaLog sagaLog) {
    this.sagaId = sagaId;
    this.requestJson = requestJson;
    this.sagaLog = sagaLog;
  }

  @Override
  public String sagaId() {
    return sagaId;
  }

  @Override
  public void commit() {
    sagaLog.offer(new SagaStartedEvent(sagaId, this));
  }

  @Override
  public void compensate() {
    sagaLog.offer(new SagaEndedEvent(sagaId, this));
  }

  @Override
  public void abort(Exception e) {

  }

  @Override
  public Transaction transaction() {
    return SAGA_START_TRANSACTION;
  }

  @Override
  public Compensation compensation() {
    return SAGA_START_COMPENSATION;
  }

  @Override
  public String serviceName() {
    return "Saga";
  }

  @Override
  public String id() {
    return "saga-start";
  }

  @Override
  public String type() {
    return "nop";
  }

  @Override
  public String json() {
    return requestJson;
  }
}
