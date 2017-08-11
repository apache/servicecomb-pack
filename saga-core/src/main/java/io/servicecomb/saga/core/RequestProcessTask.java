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

  private final EventStore eventStore;
  private final Transport transport;
  private final SagaRequest request;

  public RequestProcessTask(SagaRequest request, EventStore eventStore, Transport transport) {
    this.request = request;
    this.eventStore = eventStore;
    this.transport = transport;
  }

  @Override
  public void commit() {
    eventStore.offer(new TransactionStartedEvent(this));

    Transaction transaction = request.transaction();
    SagaResponse response = transport.with(
        request.serviceName(),
        transaction.path(),
        transaction.method(),
        transaction.params());

    eventStore.offer(new TransactionEndedEvent(this, response));
  }

  @Override
  public void compensate() {
    eventStore.offer(new CompensationStartedEvent(this));

    Compensation compensation = request.compensation();
    SagaResponse response = transport
        .with(request.serviceName(), compensation.path(), compensation.method(), compensation.params());

    eventStore.offer(new CompensationEndedEvent(this, response));
  }

  @Override
  public void abort(Exception e) {
    eventStore.offer(new TransactionAbortedEvent(this, e));
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
