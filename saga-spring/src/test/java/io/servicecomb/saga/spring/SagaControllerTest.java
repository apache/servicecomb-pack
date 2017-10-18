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

import static java.util.Collections.addAll;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.servicecomb.saga.core.SagaEndedEvent;
import io.servicecomb.saga.core.SagaStartedEvent;
import io.servicecomb.saga.core.TransactionAbortedEvent;
import io.servicecomb.saga.core.application.SagaExecutionComponent;
import io.servicecomb.saga.spring.SagaController.SagaRequest;
import io.servicecomb.saga.spring.SagaController.SagaRequestQueryResult;

@RunWith(SpringRunner.class)
@WebMvcTest(SagaController.class)
public class SagaControllerTest {

  private final String sagaId = "xxx";
  private final SagaEventEntity event = new SagaEventEntity(1L, sagaId, new Date().getTime(), "SomeEvent", "{}");
  private final Map<String, List<SagaEventEntity>> events = singletonMap(sagaId, singletonList(event));
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockBean
  private SagaExecutionComponent sagaExecutionComponent;

  @MockBean
  private SagaEventRepo repo;

  @Autowired
  private MockMvc mockMvc;

  @Before
  public void setUp() throws Exception {
    objectMapper.setVisibility(
        objectMapper.getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
  }

  @Test
  public void retrievesAllEvents() throws Exception {
    when(repo.findAll()).thenReturn(singletonList(event));

    mockMvc.perform(get("/events"))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(events)));
  }

  @Test
  public void queryRequests() throws Exception {
    long id = 0L;

    String sagaId1 = "xxxx";
    SagaEventEntity saga1StartEvent = new SagaEventEntity(++id, sagaId1, new Date().getTime(),
        SagaStartedEvent.class.getSimpleName(), "{}");
    Thread.sleep(50);
    SagaEventEntity saga1EndEvent = new SagaEventEntity(++id, sagaId1, new Date().getTime(),
        SagaEndedEvent.class.getSimpleName(), "{}");
    Thread.sleep(50);

    String sagaId2 = "yyyy";
    SagaEventEntity saga2StartEvent = new SagaEventEntity(++id, sagaId2, new Date().getTime(),
        SagaStartedEvent.class.getSimpleName(), "{}");
    Thread.sleep(50);

    String sagaId3 = "zzzz";
    SagaEventEntity saga3StartEvent = new SagaEventEntity(++id, sagaId3, new Date().getTime(),
        SagaStartedEvent.class.getSimpleName(), "{}");
    Thread.sleep(50);
    SagaEventEntity saga3TransactionAbortEvent = new SagaEventEntity(++id, sagaId3, new Date().getTime(),
        TransactionAbortedEvent.class.getSimpleName(), "{}");
    Thread.sleep(50);
    SagaEventEntity saga3EndEvent = new SagaEventEntity(++id, sagaId3, new Date().getTime(),
        SagaEndedEvent.class.getSimpleName(), "{}");
    Thread.sleep(50);

    List<SagaEventEntity> startEvents = new ArrayList<>();
    startEvents.add(saga1StartEvent);
    startEvents.add(saga2StartEvent);
    startEvents.add(saga3StartEvent);

    Pageable page = new PageRequest(1, 20);
    when(repo.findByTypeAndCreationTimeBetweenOrderByIdDesc(SagaStartedEvent.class.getSimpleName(), new Date(0),
        new Date(1), page))
        .thenReturn(new PageImpl(startEvents, page, startEvents.size()));

    when(repo.findFirstByTypeAndSagaId(SagaEndedEvent.class.getSimpleName(), sagaId1))
        .thenReturn(saga1EndEvent);
    when(repo.findFirstByTypeAndSagaId(SagaEndedEvent.class.getSimpleName(), sagaId2))
        .thenReturn(null);
    when(repo.findFirstByTypeAndSagaId(SagaEndedEvent.class.getSimpleName(), sagaId3))
        .thenReturn(saga3EndEvent);

    when(repo.findFirstByTypeAndSagaId(TransactionAbortedEvent.class.getSimpleName(), sagaId1))
        .thenReturn(null);
    when(repo.findFirstByTypeAndSagaId(TransactionAbortedEvent.class.getSimpleName(), sagaId2))
        .thenReturn(null);
    when(repo.findFirstByTypeAndSagaId(TransactionAbortedEvent.class.getSimpleName(), sagaId3))
        .thenReturn(saga3TransactionAbortEvent);

    List<SagaRequest> requests = new ArrayList<>();
    addAll(requests,
        new SagaRequest(saga1StartEvent.id(), saga1StartEvent.creationTime(), saga1EndEvent.creationTime(), "OK"),
        new SagaRequest(saga2StartEvent.id(), saga2StartEvent.creationTime(), 0, "Running"),
        new SagaRequest(saga3StartEvent.id(), saga3StartEvent.creationTime(), saga3EndEvent.creationTime(), "Failed")
    );

    SagaRequestQueryResult result = new SagaRequestQueryResult(1, 20, 2, requests);

    mockMvc.perform(get("/requests")
        .param("pageIndex", "1")
        .param("pageSize", "20")
        .param("startTime", "0")
        .param("endTime", "1"))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(result)));

    List<SagaEventEntity> saga1Events = new ArrayList<>();
    addAll(saga1Events, saga1StartEvent, saga1EndEvent);

    when(repo.findBySagaId(sagaId1))
        .thenReturn(saga1Events);

    mockMvc.perform(get("/requests/{sagaId}", sagaId1))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(saga1Events)));
  }
}