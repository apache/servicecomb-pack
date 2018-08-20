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

package org.apache.servicecomb.saga.core.dag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.servicecomb.saga.core.NoOpSagaRequest;
import org.apache.servicecomb.saga.core.SagaException;
import org.apache.servicecomb.saga.core.SagaRequest;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class GraphBuilder {

  private final GraphCycleDetector<SagaRequest> detector;

  public GraphBuilder(GraphCycleDetector<SagaRequest> detector) {
    this.detector = detector;
  }

  @Segment(name = "buildGraph", category = "application", library = "kamon")
  public SingleLeafDirectedAcyclicGraph<SagaRequest> build(SagaRequest[] sagaRequests) {
    Map<String, Node<SagaRequest>> requestNodes = requestsToNodes(sagaRequests);

    SingleLeafDirectedAcyclicGraph<SagaRequest> graph = linkNodesToGraph(sagaRequests, requestNodes);
    detectCycle(graph);
    return graph;
  }

  private SingleLeafDirectedAcyclicGraph<SagaRequest> linkNodesToGraph(
      SagaRequest[] sagaRequests,
      Map<String, Node<SagaRequest>> requestNodes) {

    Node<SagaRequest> root = rootNode(0);
    Node<SagaRequest> leaf = leafNode(sagaRequests.length + 1);

    for (SagaRequest sagaRequest : sagaRequests) {
      if (isOrphan(sagaRequest)) {
        root.addChild(requestNodes.get(sagaRequest.id()));
      } else {
        for (String parent : sagaRequest.parents()) {
          requestNodes.get(parent).addChild(requestNodes.get(sagaRequest.id()));
        }
      }
    }

    for(Node<SagaRequest> node : requestNodes.values()) {
      if (node.children().isEmpty()) {
        node.addChild(leaf);
      }
    }
    return new SingleLeafDirectedAcyclicGraph<>(root, leaf);
  }

  private Node<SagaRequest> rootNode(int id) {
    return new Node<>(
        id,
        NoOpSagaRequest.SAGA_START_REQUEST);
  }

  private Node<SagaRequest> leafNode(int id) {
    return new Node<>(
        id,
        NoOpSagaRequest.SAGA_END_REQUEST);
  }

  private boolean isOrphan(SagaRequest sagaRequest) {
    return sagaRequest.parents().length == 0;
  }

  private Map<String, Node<SagaRequest>> requestsToNodes(SagaRequest[] sagaRequests) {
    long index = 1;
    Map<String, Node<SagaRequest>> requestMap = new HashMap<>();
    for (SagaRequest sagaRequest : sagaRequests) {
      if (requestMap.containsKey(sagaRequest.id())) {
        // TODO: 8/20/2017 add random id if user didn't provide one
        throw new SagaException("Failed to interpret requests with duplicate request id: " + sagaRequest.id());
      }
      requestMap.put(sagaRequest.id(), new Node<>(index++, sagaRequest));
    }
    return requestMap;
  }

  private void detectCycle(SingleLeafDirectedAcyclicGraph<SagaRequest> graph) {
    Set<Node<SagaRequest>> jointNodes = detector.cycleJoints(graph);

    if (!jointNodes.isEmpty()) {
      throw new SagaException("Cycle detected in the request graph at nodes " + jointNodes);
    }
  }
}
