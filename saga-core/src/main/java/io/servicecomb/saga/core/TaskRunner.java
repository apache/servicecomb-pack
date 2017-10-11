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

import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;

import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.Traveller;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
  public void run(SagaResponse previousResponse) {
    Collection<Node<SagaRequest>> nodes = traveller.nodes();

    SagaResponse response = previousResponse;
    // finish pending tasks from saga log at startup
    if (!nodes.isEmpty()) {
      response = taskConsumer.consume(nodes, response);
      nodes.clear();
    }

    while (traveller.hasNext()) {
      traveller.next();
      response = taskConsumer.consume(nodes, response);
      nodes.clear();
    }
  }

  @Override
  public SagaResponse replay(Map<String, SagaResponse> completedOperations) {
    boolean played = false;
    Collection<Node<SagaRequest>> nodes = traveller.nodes();
    List<SagaResponse> previousResponses = new LinkedList<>();
    List<SagaResponse> responses = new LinkedList<>();

    while (traveller.hasNext() && !played) {
      previousResponses.clear();
      previousResponses.addAll(responses);
      responses.clear();
      traveller.next();
      played = taskConsumer.replay(nodes, completedOperations, responses);
    }
    return responseOf(previousResponses);
  }

  private SagaResponse responseOf(List<SagaResponse> responses) {
    if (responses.size() == 1) {
      return responses.get(0);
    }
    if (responses.isEmpty()) {
      return EMPTY_RESPONSE;
    }
    return new CompositeSagaResponse(responses);
  }
}
