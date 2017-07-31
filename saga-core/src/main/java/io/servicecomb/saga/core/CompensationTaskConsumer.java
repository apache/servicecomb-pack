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

import io.servicecomb.saga.core.dag.Node;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompensationTaskConsumer implements TaskConsumer {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Set<Operation> completedTransactions;

  CompensationTaskConsumer(Set<Operation> completedTransactions) {
    this.completedTransactions = completedTransactions;
  }

  @Override
  public void consume(Collection<Node<SagaTask>> nodes) {
    for (Node<SagaTask> node : nodes) {
      SagaTask task = node.value();

      if (completedTransactions.contains(task.transaction())) {
        log.info("Starting task {} id={}", task.description(), task.id());
        task.compensate();
        log.info("Completed task {} id={}", task.description(), task.id());
      }
    }
  }

  @Override
  public boolean replay(Collection<Node<SagaTask>> nodes, Set<Operation> completedOperations) {

    for (Iterator<Node<SagaTask>> iterator = nodes.iterator(); iterator.hasNext(); ) {
      SagaTask task = iterator.next().value();
      if (completedOperations.contains(task.compensation())) {
        log.info("Skipped completed compensation id={} operation={} while replay", task.id(), task.transaction());
        iterator.remove();
      } else if (!completedTransactions.contains(task.transaction())) {
        // this transaction never started
        log.info("Skipped pending transaction id={} operation={} while replay", task.id(), task.transaction());
        iterator.remove();
      }
    }
    return !nodes.isEmpty();
  }
}
