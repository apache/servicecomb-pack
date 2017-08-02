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

package io.servicecomb.saga.core.application.interpreter;

import static io.servicecomb.saga.core.Compensation.SAGA_END_COMPENSATION;
import static io.servicecomb.saga.core.Compensation.SAGA_START_COMPENSATION;
import static io.servicecomb.saga.core.Transaction.SAGA_END_TRANSACTION;
import static io.servicecomb.saga.core.Transaction.SAGA_START_TRANSACTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.TaskAwareSagaRequest;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonRequestInterpreter {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SagaTask sagaStartTask;
  private final SagaTask sagaRequestTask;
  private final SagaTask sagaEndTask;

  public JsonRequestInterpreter(
      SagaTask sagaStartTask,
      SagaTask sagaRequestTask,
      SagaTask sagaEndTask) {

    this.sagaStartTask = sagaStartTask;
    this.sagaRequestTask = sagaRequestTask;
    this.sagaEndTask = sagaEndTask;
  }

  public SingleLeafDirectedAcyclicGraph<SagaRequest> interpret(String requests) {
    try {
      JsonSagaRequest[] sagaRequests = objectMapper.readValue(requests, JsonSagaRequest[].class);

      Map<String, Node<SagaRequest>> requestNodes = requestsToNodes(sagaRequests);

      return linkNodesToGraph(sagaRequests, requestNodes);
    } catch (IOException e) {
      throw new SagaException("Failed to interpret JSON " + requests, e);
    }
  }

  private SingleLeafDirectedAcyclicGraph<SagaRequest> linkNodesToGraph(
      JsonSagaRequest[] sagaRequests,
      Map<String, Node<SagaRequest>> requestNodes) {

    Node<SagaRequest> root = rootNode(0);
    Node<SagaRequest> leaf = leafNode(sagaRequests.length + 1);

    for (JsonSagaRequest sagaRequest : sagaRequests) {
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

  private Node<SagaRequest> rootNode(int id) {
    return new Node<>(
        id,
        new TaskAwareSagaRequest("saga-start", SAGA_START_TRANSACTION, SAGA_START_COMPENSATION, sagaStartTask));
  }

  private Node<SagaRequest> leafNode(int id) {
    return new Node<>(
        id,
        new TaskAwareSagaRequest("saga-end", SAGA_END_TRANSACTION, SAGA_END_COMPENSATION, sagaEndTask));
  }

  private boolean isOrphan(JsonSagaRequest sagaRequest) {
    return sagaRequest.parents().length == 0;
  }

  private Map<String, Node<SagaRequest>> requestsToNodes(SagaRequest[] sagaRequests) {
    long index = 1;
    Map<String, Node<SagaRequest>> requestMap = new HashMap<>();
    for (SagaRequest sagaRequest : sagaRequests) {
      if (requestMap.containsKey(sagaRequest.id())) {
        throw new SagaException("Failed to interpret requests with duplicate request id: " + sagaRequest.id());
      }
      requestMap.put(sagaRequest.id(), new Node<>(index++, new TaskAwareSagaRequest(sagaRequest, sagaRequestTask)));
    }
    return requestMap;
  }
}
