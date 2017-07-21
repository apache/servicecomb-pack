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

import java.lang.invoke.MethodHandles;
import java.util.Deque;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum TransactionState implements SagaState {
  INSTANCE;
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void invoke(Deque<SagaTask> executedTasks, Queue<SagaTask> pendingTasks) {
    SagaTask task = pendingTasks.peek();
    executedTasks.push(task);

    log.info("Starting task {} id={}", task.description(), task.id());
    try {
      task.commit();
    } catch (OperationTimeoutException e) {
      log.error("Retrying timed out Transaction", e);
      task.commit();
    }
    log.info("Completed task {} id={}", task.description(), task.id());

    pendingTasks.poll();
  }

}
