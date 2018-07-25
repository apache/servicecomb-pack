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

package org.apache.servicecomb.saga.spring;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaStartedEvent;
import org.apache.servicecomb.saga.core.dag.GraphCycleDetectorImpl;
import org.apache.servicecomb.saga.core.dag.Node;
import org.apache.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import org.apache.servicecomb.saga.spring.SagaController.SagaExecution;
import org.apache.servicecomb.saga.spring.SagaController.SagaExecutionDetail;
import org.apache.servicecomb.saga.spring.SagaController.SagaExecutionQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaEndedEvent;
import org.apache.servicecomb.saga.core.TransactionAbortedEvent;
import org.apache.servicecomb.saga.core.TransactionEndedEvent;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import org.apache.servicecomb.saga.core.dag.GraphBuilder;

import org.apache.servicecomb.swagger.invocation.exception.InvocationException;

@Service
public class SagaExecutionQueryService {
  private final SagaEventRepo repo;
  private final FromJsonFormat<SagaDefinition> fromJsonFormat;
  private final SimpleDateFormat dateFormat;

  private final ObjectMapper mapper = new ObjectMapper();
  private final GraphBuilder graphBuilder = new GraphBuilder(new GraphCycleDetectorImpl<>());

  @Autowired
  public SagaExecutionQueryService(SagaEventRepo repo, FromJsonFormat<SagaDefinition> fromJsonFormat) {
    this.repo = repo;
    this.fromJsonFormat = fromJsonFormat;
    this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  public SagaExecutionQueryResult querySagaExecution(String pageIndex, String pageSize,
      String startTime, String endTime) throws ParseException {

    Date start = "NaN-NaN-NaN NaN:NaN:NaN".equals(startTime) ? new Date(0) : this.dateFormat.parse(startTime);
    Date end = "NaN-NaN-NaN NaN:NaN:NaN".equals(endTime) ? new Date() : this.dateFormat.parse(endTime);

    List<SagaExecution> requests = new ArrayList<>();
    Page<SagaEventEntity> startEvents = repo.findByTypeAndCreationTimeBetweenOrderByIdDesc(
        SagaStartedEvent.class.getSimpleName(), start, end,
        new PageRequest(Integer.parseInt(pageIndex), Integer.parseInt(pageSize)));
    for (SagaEventEntity event : startEvents) {
      SagaEventEntity endEvent = repo
          .findFirstByTypeAndSagaId(SagaEndedEvent.class.getSimpleName(), event.sagaId());
      SagaEventEntity abortedEvent = repo
          .findFirstByTypeAndSagaId(TransactionAbortedEvent.class.getSimpleName(), event.sagaId());

      requests.add(new SagaExecution(
          event.id(),
          event.sagaId(),
          event.creationTime(),
          endEvent == null ? 0 : endEvent.creationTime(),
          endEvent == null ? "Running" : abortedEvent == null ? "OK" : "Failed"));
    }

    return new SagaExecutionQueryResult(Integer.parseInt(pageIndex), Integer.parseInt(pageSize),
        startEvents.getTotalPages(), requests);
  }

  public SagaExecutionDetail querySagaExecutionDetail(String sagaId) {
    SagaEventEntity[] entities = repo.findBySagaId(sagaId).toArray(new SagaEventEntity[0]);
    Optional<SagaEventEntity> sagaStartEvent = Arrays.stream(entities)
        .filter(entity -> SagaStartedEvent.class.getSimpleName().equals(entity.type())).findFirst();
    Map<String, HashSet<String>> router = new HashMap<>();
    Map<String, String> status = new HashMap<>();
    Map<String, String> error = new HashMap<>();
    if (sagaStartEvent.isPresent()) {
      SagaDefinition definition = fromJsonFormat.fromJson(sagaStartEvent.get().contentJson());
      SingleLeafDirectedAcyclicGraph<SagaRequest> graph = graphBuilder
          .build(definition.requests());
      loopLoadGraphNodes(router, graph.root());

      Collection<SagaEventEntity> transactionAbortEvents = Arrays.stream(entities)
          .filter(entity -> TransactionAbortedEvent.class.getSimpleName().equals(entity.type())).collect(
              Collectors.toList());
      for (SagaEventEntity transactionAbortEvent : transactionAbortEvents) {
        try {
          JsonNode root = mapper.readTree(transactionAbortEvent.contentJson());
          String id = root.at("/request/id").asText();
          status.put(id, "Failed");
          error.put(id, root.at("/response/body").asText());
        } catch (IOException ex) {
          throw new InvocationException(INTERNAL_SERVER_ERROR, "illegal json content");
        }
      }

      Collection<SagaEventEntity> transactionEndEvents = Arrays.stream(entities)
          .filter(entity -> TransactionEndedEvent.class.getSimpleName().equals(entity.type())).collect(
              Collectors.toList());
      for (SagaEventEntity transactionEndEvent : transactionEndEvents) {
        try {
          JsonNode root = mapper.readTree(transactionEndEvent.contentJson());
          status.put(root.at("/request/id").asText(), "OK");
        } catch (IOException ex) {
          throw new InvocationException(INTERNAL_SERVER_ERROR, "illegal json content");
        }
      }
    }
    return new SagaExecutionDetail(router, status, error);
  }

  private void loopLoadGraphNodes(Map<String, HashSet<String>> router, Node<SagaRequest> node) {
    if (isNodeValid(node)) {
      HashSet<String> point = router.computeIfAbsent(node.value().id(), key -> new HashSet<>());
      for (Node<SagaRequest> child : node.children()) {
        point.add(child.value().id());
        loopLoadGraphNodes(router, child);
      }
    }
  }

  private boolean isNodeValid(Node<SagaRequest> node) {
    return !node.children().isEmpty();
  }
}
