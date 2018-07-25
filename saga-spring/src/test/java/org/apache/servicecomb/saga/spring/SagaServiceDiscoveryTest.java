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

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.TEXT;
import static org.apache.servicecomb.serviceregistry.client.LocalServiceRegistryClientImpl.LOCAL_REGISTRY_FILE_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import org.apache.servicecomb.saga.core.TransactionEndedEvent;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SagaSpringApplication.class)
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
  private SagaEventRepo sagaEventRepo;


  @BeforeClass
  public static void setUpClass() throws Exception {
    setUpLocalRegistry();
  }

  private static void setUpLocalRegistry() {
    System.setProperty(LOCAL_REGISTRY_FILE_KEY,"notExistJustForceLocal");
  }

  @Test
  public void processRequestByServiceDiscovery() throws Exception {
    given()
        .contentType(TEXT)
        .body(sagaDefinition)
        .when()
        .post("http://localhost:8080/requests")
        .then()
        .statusCode(200)
        .body(is("success"));

    List<SagaEventEntity> events = new ArrayList<>();
    sagaEventRepo.findAll().forEach(events::add);

    Optional<SagaEventEntity> eventEntity = events.stream()
        .filter(entity -> entity.type().equals(TransactionEndedEvent.class.getSimpleName()))
        .findFirst();

    assertThat(eventEntity.isPresent(), is(true));
    assertThat(eventEntity.get().contentJson(), containsString("hello world, with response {}"));
  }

}
