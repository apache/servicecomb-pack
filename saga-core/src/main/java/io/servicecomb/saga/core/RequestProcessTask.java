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

public class RequestProcessTask implements SagaTask, SagaRequest {

  private final String sagaId;
  private final SagaLog sagaLog;
  private final Transport transport;
  private final SagaRequest request;

  public RequestProcessTask(String sagaId, SagaRequest request, SagaLog sagaLog, Transport transport) {
    this.sagaId = sagaId;
    this.request = request;
    this.sagaLog = sagaLog;
    this.transport = transport;
  }

  @Override
  public String sagaId() {
    return sagaId;
  }

  @Override
  public void commit() {
    sagaLog.offer(new TransactionStartedEvent(sagaId, this));

    Transaction transaction = request.transaction();
    SagaResponse response = transport.with(
        request.serviceName(),
        transaction.path(),
        transaction.method(),
        transaction.params());

    sagaLog.offer(new TransactionEndedEvent(sagaId, this, response));
  }

  @Override
  public void compensate() {
    sagaLog.offer(new CompensationStartedEvent(sagaId, this));

    Compensation compensation = request.compensation();
    SagaResponse response = transport
        .with(request.serviceName(), compensation.path(), compensation.method(), compensation.params());

    sagaLog.offer(new CompensationEndedEvent(sagaId, this, response));
  }

  @Override
  public void abort(Exception e) {
    sagaLog.offer(new TransactionAbortedEvent(sagaId, this, e));
  }

  @Override
  public Transaction transaction() {
    return request.transaction();
  }

  @Override
  public Compensation compensation() {
    return request.compensation();
  }

  @Override
  public String serviceName() {
    return request.serviceName();
  }

  @Override
  public String id() {
    return request.id();
  }

  @Override
  public String type() {
    return request.type();
  }

  @Override
  public String json() {
    return request.json();
  }
}
