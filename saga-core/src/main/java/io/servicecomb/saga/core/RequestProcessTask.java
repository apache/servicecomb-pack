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

  private final EventStore eventStore;

  RequestProcessTask(EventStore eventStore) {
    this.eventStore = eventStore;
  }

  @Override
  public void commit(SagaRequest request) {
    eventStore.offer(new TransactionStartedEvent(request));
    request.transaction().run();
    eventStore.offer(new TransactionEndedEvent(request));
  }

  @Override
  public void compensate(SagaRequest request) {
    eventStore.offer(new CompensationStartedEvent(request));
    request.compensation().run();
    eventStore.offer(new CompensationEndedEvent(request));
  }

  @Override
  public void abort(SagaRequest request, Exception e) {
    eventStore.offer(new TransactionAbortedEvent(request, e));
  }
}
