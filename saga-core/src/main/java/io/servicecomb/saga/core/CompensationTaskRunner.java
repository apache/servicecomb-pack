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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompensationTaskRunner implements TaskConsumer {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<Operation, Collection<SagaEvent>> completedOperations;

  CompensationTaskRunner(Map<Operation, Collection<SagaEvent>> completedOperations) {
    this.completedOperations = completedOperations;
  }

  @Override
  public void invoke(Collection<Node<SagaTask>> nodes) {
    for (Node<SagaTask> node : nodes) {
      SagaTask task = node.value();

      if (completedOperations.containsKey(task.transaction())) {
        log.info("Starting task {} id={}", task.description(), task.id());
        task.compensate();
        log.info("Completed task {} id={}", task.description(), task.id());
      }
    }
  }

  @Override
  public boolean replay(Collection<Node<SagaTask>> nodes, Map<Operation, Collection<SagaEvent>> completedOperations) {

    for (Iterator<Node<SagaTask>> iterator = nodes.iterator(); iterator.hasNext(); ) {
      SagaTask task = iterator.next().value();
      if (completedOperations.containsKey(task.compensation())) {
        for (SagaEvent event : completedOperations.get(task.compensation())) {
          log.info("Start playing event {}", event);
          event.play(iterator);
          log.info("Completed playing event {}", event);
        }
      } else {
        iterator.remove();
      }
    }
    return !nodes.isEmpty();
  }
}
