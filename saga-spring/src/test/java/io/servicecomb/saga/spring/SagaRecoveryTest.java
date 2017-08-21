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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.spring.SagaRecoveryTest.EventPopulatingConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;
import wiremock.org.apache.http.HttpStatus;

@Ignore
@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SagaSpringApplication.class, EventPopulatingConfig.class})
public class SagaRecoveryTest {

  @ClassRule
  public static final WireMockRule wireMockRule = new WireMockRule(8090);

  private static final String requestX = "[\n"
      + "  {\n"
      + "    \"id\": \"request-xxx\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"localhost:8090\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/xxx\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"foo\": \"xxx\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/xxx\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"bar\": \"xxx\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  private static final String requestY = "[\n"
      + "  {\n"
      + "    \"id\": \"request-yyy\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"localhost:8090\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/yyy\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"foo\": \"yyy\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/yyy\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"bar\": \"yyy\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  @Autowired
  private SagaEventRepo sagaEventRepo;

  @BeforeClass
  public static void setUp() throws Exception {
    stubFor(WireMock.post(urlPathEqualTo("/rest/yyy"))
        .withRequestBody(containing("foo=yyy"))
        .willReturn(
            aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody("success")));
  }

  @Test
  public void recoverIncompleteSagasFromSagaLog() throws Exception {
    verify(exactly(0), postRequestedFor(urlPathEqualTo("/rest/xxx")));
    verify(exactly(1), postRequestedFor(urlPathEqualTo("/rest/yyy")));
  }

  @Configuration
  static class EventPopulatingConfig {

    @Primary
    @Bean
    PersistentStore persistentStore(SagaEventRepo repo) {
      repo.save(new SagaEventEntity("xxx", "SagaStartedEvent", requestX));
      repo.save(new SagaEventEntity("xxx", "TransactionStartedEvent", "{}"));
      repo.save(new SagaEventEntity("xxx", "TransactionEndedEvent", "{}"));
      repo.save(new SagaEventEntity("xxx", "SagaEndedEvent", "{}"));

      repo.save(new SagaEventEntity("yyy", "SagaStartedEvent", requestY));

      return new JpaPersistentStore(repo);
    }
  }
}
