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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.servicecomb.saga.core.SagaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import io.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.saga.core.SagaException;
import org.apache.servicecomb.saga.core.application.SagaExecutionComponent;
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
  private final SagaExecutionQueryService queryService;
  private final SimpleDateFormat dateFormat;

  @Autowired
  public SagaController(SagaExecutionComponent sagaExecutionComponent, SagaEventRepo repo,
      SagaExecutionQueryService queryService) {
    this.sagaExecutionComponent = sagaExecutionComponent;
    this.repo = repo;
    this.queryService = queryService;
    this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  @Trace("processRequests")
  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "success"),
      @ApiResponse(code = 400, response = String.class, message = "illegal request content"),
      @ApiResponse(code = 500, response = String.class, message = "transaction failed")
  })
  @CrossOrigin
  @RequestMapping(value = "requests", method = POST, consumes = TEXT_PLAIN_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> processRequests(@RequestBody String request) {
    try {
      SagaResponse response = sagaExecutionComponent.run(request);
      if (response.succeeded()) {
        return ResponseEntity.ok("success");
      } else {
        throw new InvocationException(INTERNAL_SERVER_ERROR, response.body());
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
  @CrossOrigin
  @RequestMapping(value = "requests", method = GET)
  public ResponseEntity<SagaExecutionQueryResult> queryExecutions(
      @RequestParam(name = "pageIndex") String pageIndex,
      @RequestParam(name = "pageSize") String pageSize,
      @RequestParam(name = "startTime") String startTime,
      @RequestParam(name = "endTime") String endTime) {
    if (isRequestParamValid(pageIndex, pageSize, startTime, endTime)) {
      try {
        return ResponseEntity.ok(queryService.querySagaExecution(pageIndex, pageSize, startTime, endTime));
      } catch (ParseException ignored) {
        throw new InvocationException(BAD_REQUEST, "illegal request content");
      }
    } else {
      throw new InvocationException(BAD_REQUEST, "illegal request content");
    }
  }

  private boolean isRequestParamValid(String pageIndex, String pageSize, String startTime, String endTime) {
    try {
      if (Integer.parseInt(pageIndex) >= 0 && Integer.parseInt(pageSize) > 0) {
        Date start = "NaN-NaN-NaN NaN:NaN:NaN".equals(startTime) ? new Date(0) : this.dateFormat.parse(startTime);
        Date end =
            "NaN-NaN-NaN NaN:NaN:NaN".equals(endTime) ? new Date(Long.MAX_VALUE) : this.dateFormat.parse(endTime);
        return start.getTime() <= end.getTime();
      }
    } catch (NumberFormatException | ParseException ignored) {
    }
    return false;
  }

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "success"),
  })
  @CrossOrigin
  @RequestMapping(value = "requests/{sagaId}", method = GET)
  public ResponseEntity<SagaExecutionDetail> queryExecutionDetail(@PathVariable String sagaId) {
    return ResponseEntity.ok(queryService.querySagaExecutionDetail(sagaId));
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  private static class SagaEventVo extends SagaEventEntity {

    private SagaEventVo(long id, String sagaId, long creationTime, String type, String contentJson) {
      super(id, sagaId, creationTime, type, contentJson);
    }
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  static class SagaExecutionQueryResult {
    public int pageIndex;
    public int pageSize;
    public int totalPages;

    public List<SagaExecution> requests;

    public SagaExecutionQueryResult() {
    }

    public SagaExecutionQueryResult(int pageIndex, int pageSize, int totalPages, List<SagaExecution> requests) {
      this();
      this.pageIndex = pageIndex;
      this.pageSize = pageSize;
      this.totalPages = totalPages;
      this.requests = requests;
    }
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  static class SagaExecution {
    public long id;
    public String sagaId;
    public long startTime;
    public String status;
    public long completedTime;

    public SagaExecution() {
    }

    public SagaExecution(long id, String sagaId, long startTime, long completedTime, String status) {
      this();
      this.id = id;
      this.sagaId = sagaId;
      this.startTime = startTime;
      this.completedTime = completedTime;
      this.status = status;
    }
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  static class SagaExecutionDetail {
    public Map<String, HashSet<String>> router;
    public Map<String, String> status;
    public Map<String, String> error;

    public SagaExecutionDetail() {
    }

    public SagaExecutionDetail(Map<String, HashSet<String>> router, Map<String, String> status,
        Map<String, String> error) {
      this();
      this.router = router;
      this.status = status;
      this.error = error;
    }
  }
}
