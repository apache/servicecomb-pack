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

package io.servicecomb.saga.transports.httpclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.TransactionFailedException;
import io.servicecomb.saga.core.Transport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class HttpClientTransportTest {

  @ClassRule
  public static final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private static final String usableResource = "/rest/usableResource";
  private static final String faultyResource = "/rest/faultyResource";
  private static final String usableResponse = "hello world";
  private static final String faultyResponse = "no such resource";
  private static final String json = "{\"hello\", \"world\"}";

  private String serviceName;

  private final Transport transport = new HttpClientTransport();

  @BeforeClass
  public static void setUpClass() throws Exception {
    stubFor(get(urlPathEqualTo(usableResource))
        .withQueryParam("foo", equalTo("bar"))
        .withQueryParam("hello", equalTo("world"))
        .willReturn(
            aResponse()
                .withStatus(SC_OK)
                .withBody(usableResponse)));

    stubFor(post(urlPathEqualTo(faultyResource))
        .withQueryParam("foo", equalTo("bar"))
        .withRequestBody(containing("hello=world&jesus=christ"))
        .willReturn(
            aResponse()
                .withStatus(SC_INTERNAL_SERVER_ERROR)
                .withBody(faultyResponse)));

    stubFor(put(urlPathEqualTo(usableResource))
        .withQueryParam("foo", equalTo("bar"))
        .withRequestBody(equalTo(json))
        .willReturn(
            aResponse()
                .withStatus(SC_OK)
                .withBody(usableResponse)));
  }

  @Before
  public void setUp() throws Exception {
    serviceName = "localhost" + ":" + wireMockRule.port();
  }

  @Test
  public void getsRequestFromRemote() {
    Map<String, Map<String, String>> requests = singletonMap("query", map("foo", "bar", "hello", "world"));

    SagaResponse response = transport.with(serviceName, usableResource, "GET", requests);

    assertThat(response.succeeded(), is(true));
    assertThat(response.body(), allOf(
        containsString(usableResponse),
        containsString(String.valueOf(SC_OK))));
  }

  @Test
  public void putsRequestToRemote() {
    Map<String, Map<String, String>> requests = new HashMap<>();
    requests.put("query", singletonMap("foo", "bar"));
    requests.put("json", singletonMap("body", json));

    SagaResponse response = transport.with(serviceName, usableResource, "PUT", requests);

    assertThat(response.succeeded(), is(true));
    assertThat(response.body(), allOf(
        containsString(usableResponse),
        containsString(String.valueOf(SC_OK))));
  }

  @Test
  public void blowsUpWhenRemoteResponseIsNot2XX() {
    Map<String, Map<String, String>> requests = new HashMap<>();
    requests.put("query", singletonMap("foo", "bar"));
    requests.put("form", map("hello", "world", "jesus", "christ"));

    try {
      transport.with(serviceName, faultyResource, "POST", requests);
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getMessage(), containsString("The remote service returned with status code 500"));
    }
  }

  @Test
  public void blowsUpWhenRemoteIsNotReachable() {
    try {
      transport.with("http://somewhere:9090", faultyResource, "DELETE", emptyMap());
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getMessage(), is("Network Error"));
    }
  }

  @Test
  public void blowsUpWhenMethodIsUnknown() {
    try {
      transport.with(serviceName, usableResource, "Blah", emptyMap());
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getMessage(), is("No such method Blah"));
    }
  }

  @Test
  public void blowsUpWhenUriIsMalformed() {
    try {
      transport.with("\\", usableResource, "GET", emptyMap());
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getMessage(), is("Wrong request URI"));
    }
  }

  private Map<String, String> map(String... pairs) {
    return new LinkedHashMap<String, String>(){{
      for (int i = 0; i < pairs.length; i+=2) {
        put(pairs[i], pairs[i + 1]);
      }
    }};
  }
}