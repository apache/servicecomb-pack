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

package io.servicecomb.saga.core.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.servicecomb.saga.core.NoOpSagaRequest;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.dag.GraphCycleDetector;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
class GraphBuilder {

  private final GraphCycleDetector<SagaResponse, SagaRequest> detector;

  GraphBuilder(GraphCycleDetector<SagaResponse, SagaRequest> detector) {
    this.detector = detector;
  }

  @Segment(name = "buildGraph", category = "application", library = "kamon")
  SingleLeafDirectedAcyclicGraph<SagaResponse, SagaRequest> build(SagaRequest[] sagaRequests) {
    Map<String, Node<SagaResponse, SagaRequest>> requestNodes = requestsToNodes(sagaRequests);

    SingleLeafDirectedAcyclicGraph<SagaResponse, SagaRequest> graph = linkNodesToGraph(sagaRequests, requestNodes);
    detectCycle(graph);
    return graph;
  }

  private SingleLeafDirectedAcyclicGraph<SagaResponse, SagaRequest> linkNodesToGraph(
      SagaRequest[] sagaRequests,
      Map<String, Node<SagaResponse, SagaRequest>> requestNodes) {

    Node<SagaResponse, SagaRequest> root = rootNode();
    Node<SagaResponse, SagaRequest> leaf = leafNode(sagaRequests.length + 1);

    for (SagaRequest sagaRequest : sagaRequests) {
      if (isOrphan(sagaRequest)) {
        root.addChild(requestNodes.get(sagaRequest.id()));
      } else {
        for (String parent : sagaRequest.parents()) {
          requestNodes.get(parent).addChild(requestNodes.get(sagaRequest.id()));
        }
      }
    }

    requestNodes.values().stream()
        .filter((node) -> node.children().isEmpty())
        .forEach(node -> node.addChild(leaf));

    return new SingleLeafDirectedAcyclicGraph<>(root, leaf);
  }

  private Node<SagaResponse, SagaRequest> rootNode() {
    return new Node<>(
        0,
        NoOpSagaRequest.SAGA_START_REQUEST);
  }

  private Node<SagaResponse, SagaRequest> leafNode(int id) {
    return new Node<>(
        id,
        NoOpSagaRequest.SAGA_END_REQUEST);
  }

  private boolean isOrphan(SagaRequest sagaRequest) {
    return sagaRequest.parents().length == 0;
  }

  private Map<String, Node<SagaResponse, SagaRequest>> requestsToNodes(SagaRequest[] sagaRequests) {
    long index = 1;
    Map<String, Node<SagaResponse, SagaRequest>> requestMap = new HashMap<>();
    for (SagaRequest sagaRequest : sagaRequests) {
      if (requestMap.containsKey(sagaRequest.id())) {
        // TODO: 8/20/2017 add random id if user didn't provide one
        throw new SagaException("Failed to interpret requests with duplicate request id: " + sagaRequest.id());
      }
      requestMap.put(sagaRequest.id(), new Node<>(index++, sagaRequest));
    }
    return requestMap;
  }

  private void detectCycle(SingleLeafDirectedAcyclicGraph<SagaResponse, SagaRequest> graph) {
    Set<Node<SagaResponse, SagaRequest>> jointNodes = detector.cycleJoints(graph);

    if (!jointNodes.isEmpty()) {
      throw new SagaException("Cycle detected in the request graph at nodes " + jointNodes);
    }
  }
}