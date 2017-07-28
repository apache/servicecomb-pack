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

class SagaStartTask implements SagaTask {

  private final long id;
  private final IdGenerator<Long> idGenerator;
  private final EventStore eventStore;

  SagaStartTask(long id, EventStore eventStore, IdGenerator<Long> idGenerator) {
    this.id = id;

    this.idGenerator = idGenerator;
    this.eventStore = eventStore;
  }

  @Override
  public long id() {
    return id;
  }

  @Override
  public Operation transaction() {
    return Operation.NO_OP;
  }

  @Override
  public void commit() {
    eventStore.offer(new SagaStartedEvent(idGenerator.nextId(), this));
  }

  @Override
  public void compensate() {
    eventStore.offer(new SagaEndedEvent(idGenerator.nextId(), this));
  }

  @Override
  public void abort() {

  }

  @Override
  public Operation compensation() {
    return Operation.END_OP;
  }
}
