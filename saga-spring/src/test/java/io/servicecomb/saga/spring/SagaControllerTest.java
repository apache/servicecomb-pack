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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.servicecomb.saga.core.application.SagaExecutionComponent;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

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
}