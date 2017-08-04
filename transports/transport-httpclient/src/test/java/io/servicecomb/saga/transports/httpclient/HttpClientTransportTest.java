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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.Transport;
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

  private String serviceName;

  private final Transport transport = new HttpClientTransport();

  @BeforeClass
  public static void setUpClass() throws Exception {
    stubFor(get(urlEqualTo(usableResource))
        .willReturn(
            aResponse()
                .withStatus(SC_OK)
                .withBody(usableResponse)));

    stubFor(post(urlEqualTo(faultyResource))
        .willReturn(
            aResponse()
                .withStatus(SC_INTERNAL_SERVER_ERROR)
                .withBody(faultyResponse)));
  }

  @Before
  public void setUp() throws Exception {
    serviceName = "localhost" + ":" + wireMockRule.port();
  }

  @Test
  public void sendsRequestToRemote() {
    SagaResponse response = transport.with(serviceName, usableResource, "GET");

    assertThat(response.succeeded(), is(true));
    assertThat(response.body(), allOf(
        containsString(usableResponse),
        containsString(String.valueOf(SC_OK))));
  }

  @Test
  public void blowsUpWhenRemoteResponseIsNot2XX() {
    SagaResponse response = transport.with(serviceName, faultyResource, "POST");

    assertThat(response.succeeded(), is(false));
    assertThat(response.body(), allOf(
        containsString(faultyResponse),
        containsString(String.valueOf(SC_INTERNAL_SERVER_ERROR)),
        containsString("Server Error")));
  }

  @Test
  public void blowsUpWhenRemoteIsNotReachable() {
    SagaResponse response = transport.with("http://somewhere:9090", faultyResource, "PUT");

    assertThat(response.succeeded(), is(false));
    assertThat(response.body(), allOf(
        containsString("java.net.UnknownHostException: http"),
        containsString("0"),
        containsString("Network Error")));
  }
}