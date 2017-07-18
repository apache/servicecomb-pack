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

public class Sage {

  private final IdGenerator<Long> idGenerator;
  private final EventQueue eventQueue;
  private final Transaction[] transactions;

  public Sage(IdGenerator<Long> idGenerator, EventQueue eventQueue, Transaction... transactions) {
    this.idGenerator = idGenerator;
    this.eventQueue = eventQueue;
    this.transactions = transactions;
  }

  public void run() {
    eventQueue.offer(new SagaStartedEvent(idGenerator.nextId()));

    for (Transaction transaction : transactions) {
      eventQueue.offer(new TransactionStartedEvent(idGenerator.nextId(), transaction));
      transaction.execute();
      eventQueue.offer(new TransactionEndedEvent(idGenerator.nextId(), transaction));
    }

    eventQueue.offer(new SagaEndedEvent(idGenerator.nextId()));
  }
}
