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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLEncoder;

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

@SuppressWarnings("unchecked")
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
      + "  },\n"
      + "  {\n"
      + "    \"id\": \"request-bbb\",\n"
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
      + "    },\n"
      + "    \"parents\": [\"request-aaa\"]\n"
      + "  }\n"
      + "]\n";

  private static final String failRequests = "[\n"
      + "  {\n"
      + "    \"id\": \"request-aaa\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"localhost:8090\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/bs\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"foo\": \"bs\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/bs\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"bar\": \"bs\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  private static final String sagaDefinition = "{\"policy\": \"ForwardRecovery\",\"requests\": " + requests + "}";
  private static final String sagaFailDefinition =
      "{\"policy\": \"BackwardRecovery\",\"requests\": " + failRequests + "}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SagaEventRepo sagaEventRepo;

  @BeforeClass
  public static void setUp() throws Exception {
    stubFor(WireMock.post(urlPathEqualTo("/rest/as"))
        .withRequestBody(containing("foo=as&response=" + URLEncoder.encode("{}", "UTF-8")))
        .willReturn(
            aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody("{\n"
                    + "  \"body\": \"success\",\n"
                    + "  \"sagaChildren\": [\"none\"]\n"
                    + "}")));

    stubFor(WireMock.post(urlPathEqualTo("/rest/bs"))
        .withRequestBody(containing("foo=bs"))
        .willReturn(
            aResponse()
                .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .withBody("process failed")));

  }

  @Test
  public void testBadFormatRequest() throws Exception {
    try {
      mockMvc.perform(
          post("/requests/")
              .contentType(TEXT_PLAIN)
              .content("xxxx"))
          .andExpect(status().is(HttpStatus.SC_BAD_REQUEST))
          .andExpect(content().string(containsString("illegal request content")));
    } catch (org.springframework.web.util.NestedServletException ex) {
      assertThat(ex.getMessage(), containsString("Failed to interpret JSON xxxx"));
    }
  }

  @Test
  public void testFailedRequest() throws Exception {
    try {
      mockMvc.perform(
          post("/requests/")
              .contentType(TEXT_PLAIN)
              .content(sagaFailDefinition))
          .andExpect(status().is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
          .andExpect(content().string(containsString("transaction failed")));
    } catch (org.springframework.web.util.NestedServletException ex) {
      assertThat(ex.getMessage(), containsString(
          "The remote service returned with status code 500, reason Server Error"));
    }
  }

  @Test
  public void processRequestByRest() throws Exception {
    mockMvc.perform(
        post("/requests/")
            .contentType(TEXT_PLAIN)
            .content(sagaDefinition))
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

  @Test
  public void queryRequestsWithBadParameter() throws Exception {
    try {
    mockMvc.perform(get("/requests")
        .param("pageIndex", "xxx")
        .param("pageSize", "xxx")
        .param("startTime", "xx0")
        .param("endTime", "x1"))
        .andExpect(status().is(HttpStatus.SC_BAD_REQUEST));
    } catch (org.springframework.web.util.NestedServletException ex) {
      assertThat(ex.getMessage(), containsString(
          "java.lang.NumberFormatException"));
    }
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
