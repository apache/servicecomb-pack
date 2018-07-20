/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.core;

import org.apache.servicecomb.saga.core.dag.Node;
import org.apache.servicecomb.saga.core.dag.Traveller;

import java.util.Collection;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
class TaskRunner implements SagaState {

  private final Traveller<SagaRequest> traveller;
  private final TaskConsumer taskConsumer;

  TaskRunner(Traveller<SagaRequest> traveller, TaskConsumer taskConsumer) {
    this.traveller = traveller;
    this.taskConsumer = taskConsumer;
  }

  @Override
  public boolean hasNext() {
    return traveller.hasNext();
  }

  @Segment(name = "runTask", category = "application", library = "kamon")
  @Override
  public void run() {
    Collection<Node<SagaRequest>> nodes = traveller.nodes();

    // finish pending tasks from saga log at startup
    if (!nodes.isEmpty()) {
      taskConsumer.consume(nodes);
      nodes.clear();
    }

    while (traveller.hasNext()) {
      traveller.next();
      taskConsumer.consume(nodes);
      nodes.clear();
    }
  }

  @Override
  public void replay() {
    boolean played = false;
    Collection<Node<SagaRequest>> nodes = traveller.nodes();

    while (traveller.hasNext() && !played) {
      traveller.next();
      played = taskConsumer.replay(nodes);
    }
  }
}
