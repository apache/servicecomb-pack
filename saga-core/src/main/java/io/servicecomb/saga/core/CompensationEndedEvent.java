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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class CompensationEndedEvent extends SagaEvent {

  CompensationEndedEvent(SagaTask compensation) {
    super(compensation);
  }

  @Override
  public void gatherTo(
      Set<SagaTask> hangingTransactions,
      Set<SagaTask> abortedTransactions,
      Map<Operation, Collection<SagaEvent>> completedTransactions,
      Map<Operation, Collection<SagaEvent>> completedCompensations) {

    completedCompensations.get(payload().compensation()).add(this);
    hangingTransactions.remove(payload());
  }

  @Override
  public void play(Iterator<Node<SagaTask>> iterator) {
    iterator.remove();
  }

  @Override
  public String toString() {
    return "CompensationEndedEvent{id="
        + payload().id()
        + ", operation="
        + payload().compensation()
        + "}";
  }
}
