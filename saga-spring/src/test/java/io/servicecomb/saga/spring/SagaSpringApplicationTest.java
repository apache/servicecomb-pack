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
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import wiremock.org.apache.http.HttpStatus;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SagaSpringApplication.class)
@AutoConfigureMockMvc
public class SagaSpringApplicationTest {

  @ClassRule
  public static final WireMockRule wireMockRule = new WireMockRule(8090);

  private static final String requests = "[\n"
      + "  {\n"
      + "    \"id\": \"request-aaa\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"localhost:8090\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/as\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"foo\": \"as\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/as\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"bar\": \"as\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SagaEventRepo sagaEventRepo;

  @BeforeClass
  public static void setUp() throws Exception {
    stubFor(WireMock.post(urlPathEqualTo("/rest/as"))
        .withRequestBody(containing("foo=as"))
        .willReturn(
            aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody("success")));
  }

  @Test
  public void processRequestByRest() throws Exception {
    mockMvc.perform(
        post("/requests/")
            .contentType(APPLICATION_JSON)
            .content(requests))
        .andExpect(status().isOk());

    verify(exactly(1), postRequestedFor(urlPathEqualTo("/rest/as")));

    Iterable<SagaEventEntity> events = sagaEventRepo.findAll();

    assertThat(events, contains(
        eventWith(1L, "SagaStartedEvent"),
        eventWith(2L, "TransactionStartedEvent"),
        eventWith(3L, "TransactionEndedEvent"),
        eventWith(4L, "SagaEndedEvent")
    ));
  }

  private Matcher<SagaEventEntity> eventWith(
      long eventId,
      String type) {

    return new TypeSafeMatcher<SagaEventEntity>() {
      @Override
      protected boolean matchesSafely(SagaEventEntity event) {
        return eventId == event.id() && event.type().equals(type);
      }

      @Override
      protected void describeMismatchSafely(SagaEventEntity item, Description mismatchDescription) {
        mismatchDescription.appendText(item.toString());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(
            "SagaEventEntity {"
                + "id=" + eventId
                + ", type=" + type);
      }
    };
  }
}
