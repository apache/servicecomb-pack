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

package io.servicecomb.saga.spring;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import io.servicecomb.provider.rest.common.RestSchema;
import io.servicecomb.saga.core.SagaEndedEvent;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaStartedEvent;
import io.servicecomb.saga.core.TransactionAbortedEvent;
import io.servicecomb.saga.core.application.SagaExecutionComponent;
import io.servicecomb.swagger.invocation.exception.InvocationException;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import kamon.annotation.EnableKamon;
import kamon.annotation.Trace;

@EnableKamon
@Controller
@RequestMapping("/")
@RestSchema(schemaId = "saga-endpoint")
public class SagaController {

  private final SagaExecutionComponent sagaExecutionComponent;
  private final SagaEventRepo repo;

  @Autowired
  public SagaController(SagaExecutionComponent sagaExecutionComponent, SagaEventRepo repo) {
    this.sagaExecutionComponent = sagaExecutionComponent;
    this.repo = repo;
  }

  @Trace("processRequests")
  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "success"),
      @ApiResponse(code = 400, response = String.class, message = "illegal request content"),
      @ApiResponse(code = 500, response = String.class, message = "transaction failed")
  })
  @RequestMapping(value = "requests", method = POST, consumes = TEXT_PLAIN_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> processRequests(@RequestBody String request) {
    try {
      String runResult = sagaExecutionComponent.run(request);
      if (runResult == null) {
        return ResponseEntity.ok("success");
      } else {
        throw new InvocationException(INTERNAL_SERVER_ERROR, runResult);
      }
    } catch (SagaException se) {
      throw new InvocationException(BAD_REQUEST, se);
    }
  }

  @RequestMapping(value = "events", method = GET)
  public ResponseEntity<Map<String, List<SagaEventVo>>> allEvents() {
    Iterable<SagaEventEntity> entities = repo.findAll();

    Map<String, List<SagaEventVo>> events = new LinkedHashMap<>();
    entities.forEach(e -> {
      events.computeIfAbsent(e.sagaId(), id -> new LinkedList<>());
      events.get(e.sagaId()).add(new SagaEventVo(e.id(), e.sagaId(), e.creationTime(), e.type(), e.contentJson()));
    });

    return ResponseEntity.ok(events);
  }

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "success"),
      @ApiResponse(code = 400, response = String.class, message = "illegal request content"),
  })
  @RequestMapping(value = "requests", method = GET)
  public ResponseEntity<SagaRequestQueryResult> queryRequests(
      @RequestParam(name = "pageIndex") String pageIndex,
      @RequestParam(name = "pageSize") String pageSize,
      @RequestParam(name = "startTime") String startTime,
      @RequestParam(name = "endTime") String endTime) {
    if (isRequestParamValid(pageIndex, pageSize, startTime, endTime)) {
      List<SagaRequest> requests = new ArrayList<>();
      Page<SagaEventEntity> startEvents = repo.findByTypeAndCreationTimeBetweenOrderByIdDesc(
          SagaStartedEvent.class.getSimpleName(),
          new Date(Long.parseLong(startTime)), new Date(Long.parseLong(endTime)),
          new PageRequest(Integer.parseInt(pageIndex), Integer.parseInt(pageSize)));
      for (SagaEventEntity event : startEvents) {
        SagaEventEntity endEvent = repo
            .findFirstByTypeAndSagaId(SagaEndedEvent.class.getSimpleName(), event.sagaId());
        SagaEventEntity abortedEvent = repo
            .findFirstByTypeAndSagaId(TransactionAbortedEvent.class.getSimpleName(), event.sagaId());

        requests.add(new SagaRequest(
            event.id(),
            event.creationTime(),
            endEvent == null ? 0 : endEvent.creationTime(),
            endEvent == null ? "Running" : abortedEvent == null ? "OK" : "Failed"));
      }
      return ResponseEntity.ok(
          new SagaRequestQueryResult(Integer.parseInt(pageIndex), Integer.parseInt(pageSize),
              startEvents.getTotalPages(), requests));
    } else {
      throw new InvocationException(BAD_REQUEST, "illegal request content");
    }
  }

  private boolean isRequestParamValid(String pageIndex, String pageSize, String startTime, String endTime) {
    return Integer.parseInt(pageIndex) >= 0 && Integer.parseInt(pageSize) > 0
        && Long.parseLong(startTime) <= Long.parseLong(endTime);
  }

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "success"),
      @ApiResponse(code = 400, response = String.class, message = "illegal request content"),
  })
  @RequestMapping(value = "requests/{sagaId}", method = GET)
  public ResponseEntity<List<SagaEventVo>> queryRequest(@PathVariable String sagaId) {
    Iterable<SagaEventEntity> entities = repo.findBySagaId(sagaId);
    List<SagaEventVo> events = new ArrayList<>();
    entities
        .forEach(e -> events.add(new SagaEventVo(e.id(), e.sagaId(), e.creationTime(), e.type(), e.contentJson())));
    return ResponseEntity.ok(events);
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  private static class SagaEventVo extends SagaEventEntity {

    private SagaEventVo(long id, String sagaId, long creationTime, String type, String contentJson) {
      super(id, sagaId, creationTime, type, contentJson);
    }
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  protected static class SagaRequestQueryResult {
    public int pageIndex;
    public int pageSize;
    public int totalPages;

    public List<SagaRequest> requests = null;

    public SagaRequestQueryResult(int pageIndex, int pageSize, int totalPages, List<SagaRequest> requests) {
      this.pageIndex = pageIndex;
      this.pageSize = pageSize;
      this.totalPages = totalPages;
      this.requests = requests;
    }
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  protected static class SagaRequest {
    public long id;
    public long startTime;
    public String status;
    public long completedTime;

    public SagaRequest(long id, long startTime, long completedTime, String status) {
      this.id = id;
      this.startTime = startTime;
      this.completedTime = completedTime;
      this.status = status;
    }
  }
}
