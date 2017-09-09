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

import static io.servicecomb.serviceregistry.client.LocalServiceRegistryClientImpl.LOCAL_REGISTRY_FILE_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.servicecomb.saga.core.TransactionEndedEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SagaSpringApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("servicecomb")
public class SagaServiceDiscoveryTest {

  private static final String requests = "[\n"
      + "  {\n"
      + "    \"id\": \"request-aaa\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"saga-service\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/usableResource\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"hello\": \"world\"\n"
      + "        }"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/usableResource\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"foo\": \"bar\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  private static final String sagaDefinition = "{\"policy\": \"ForwardRecovery\",\"requests\": " + requests + "}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SagaEventRepo sagaEventRepo;


  @BeforeClass
  public static void setUpClass() throws Exception {
    setUpLocalRegistry();
  }

  private static void setUpLocalRegistry() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL resource = loader.getResource("registry.yaml");
    System.setProperty(LOCAL_REGISTRY_FILE_KEY, resource.getPath());
  }

  @Test
  public void processRequestByServiceDiscovery() throws Exception {
    mockMvc.perform(
        post("/requests/")
            .contentType(APPLICATION_JSON)
            .content(sagaDefinition))
        .andExpect(status().isOk());

    List<SagaEventEntity> events = new ArrayList<>();
    sagaEventRepo.findAll().forEach(events::add);

    Optional<SagaEventEntity> eventEntity = events.stream()
        .filter(entity -> entity.type().equals(TransactionEndedEvent.class.getSimpleName()))
        .findFirst();

    assertThat(eventEntity.isPresent(), is(true));
    assertThat(eventEntity.get().contentJson(), containsString("hello world"));
  }

}
