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

class RequestProcessTask implements SagaTask {

  private final EventQueue eventQueue;
  private final IdGenerator<Long> idGenerator;
  private final long id;
  private final SagaRequest request;

  RequestProcessTask(long id, SagaRequest request, EventQueue eventQueue,
      IdGenerator<Long> idGenerator) {
    this.id = id;
    this.request = request;
    this.eventQueue = eventQueue;
    this.idGenerator = idGenerator;
  }

  @Override
  public long id() {
    return id;
  }

  @Override
  public void commit() {
    eventQueue.offer(new TransactionStartedEvent(idGenerator.nextId(), request.transaction()));
    request.commit();
    eventQueue.offer(new TransactionEndedEvent(idGenerator.nextId(), request.transaction()));
  }

  @Override
  public void abort() {
    eventQueue.offer(new CompensationStartedEvent(idGenerator.nextId(), request.compensation()));
    request.abort();
    eventQueue.offer(new CompensationEndedEvent(idGenerator.nextId(), request.compensation()));
  }
}
