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
import io.servicecomb.saga.core.dag.Traveller;
import java.util.Collection;
import java.util.Map;

abstract class AbstractSagaState implements SagaState {

  private final Traveller<SagaTask> traveller;

  AbstractSagaState(Traveller<SagaTask> traveller) {
    this.traveller = traveller;
  }

  @Override
  public boolean hasNext() {
    return traveller.hasNext();
  }

  @Override
  public void run() {
    Collection<Node<SagaTask>> nodes = traveller.nodes();

    // finish pending tasks from saga log at startup
    invoke(nodes);
    nodes.clear();

    while (traveller.hasNext()) {
      traveller.next();
      invoke(nodes);
      nodes.clear();
    }
  }

  abstract void invoke(Collection<Node<SagaTask>> nodes);

  @Override
  public void replay(Map<Operation, Collection<SagaEvent>> completedOperations) {
    boolean played = false;
    Collection<Node<SagaTask>> nodes = traveller.nodes();
    while (traveller.hasNext() && !played) {
      traveller.next();
      played = replay(nodes, completedOperations);
    }
  }

  abstract boolean replay(Collection<Node<SagaTask>> nodes,
      Map<Operation, Collection<SagaEvent>> completedOperationsCopy);

}
