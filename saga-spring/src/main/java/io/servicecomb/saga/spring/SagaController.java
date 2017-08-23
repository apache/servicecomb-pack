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
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.servicecomb.saga.core.application.SagaCoordinator;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class SagaController {

  private final SagaCoordinator sagaCoordinator;
  private final SagaEventRepo repo;

  @Autowired
  public SagaController(SagaCoordinator sagaCoordinator, SagaEventRepo repo) {
    this.sagaCoordinator = sagaCoordinator;
    this.repo = repo;
  }

  @RequestMapping(value = "requests", method = POST, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> processRequests(HttpServletRequest request) {
    try {
      sagaCoordinator.run(IOUtils.toString(request.getInputStream()));
      return ResponseEntity.ok("success");
    } catch (IOException e) {
      return new ResponseEntity<>(e.getMessage(), INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(value = "events", method = GET)
  public ResponseEntity<Map<String, List<SagaEventVo>>> allEvents() {
    Iterable<SagaEventEntity> entities = repo.findAll();

    Map<String, List<SagaEventVo>> events = new HashMap<>();
    entities.forEach(e -> {
      events.computeIfAbsent(e.sagaId(), id -> new LinkedList<>());
      events.get(e.sagaId()).add(new SagaEventVo(e.id(), e.sagaId(), e.creationTime(), e.type(), e.contentJson()));
    });

    return ResponseEntity.ok(events);
  }

  @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
  private static class SagaEventVo extends SagaEventEntity {

    private SagaEventVo(long id, String sagaId, long creationTime, String type, String contentJson) {
      super(id, sagaId, creationTime, type, contentJson);
    }
  }
}
