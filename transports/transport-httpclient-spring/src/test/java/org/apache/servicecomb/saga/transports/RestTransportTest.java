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

package org.apache.servicecomb.saga.transports;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.util.Collections.emptyMap;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.SocketTimeoutException;

import org.apache.servicecomb.saga.core.SagaResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.servicecomb.saga.core.TransportFailedException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestTransportTest.Config.class)
public class RestTransportTest {

  @Autowired
  RestTransport transport;

  @ClassRule
  public static final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private static final String normalResource = "/rest/normalResource";
  private static final String slowResource = "/rest/slowResource";

  private String address;

  @BeforeClass
  public static void init() throws Exception {
    System.setProperty("saga.request.timeout", "1000");

    stubFor(get(urlPathEqualTo(normalResource))
        .willReturn(
            aResponse()
                .withStatus(SC_OK)));

    stubFor(get(urlPathEqualTo(slowResource))
        .willReturn(
            aResponse()
                .withStatus(SC_OK)
                .withFixedDelay(2000)));
  }

  @AfterClass
  public static void shutdown() throws Exception {
    System.clearProperty("saga.request.timeout");
  }

  @Before
  public void setUp() throws Exception {
    address = "localhost" + ":" + wireMockRule.port();
  }

  @Test
  public void ensureNormalRequestWorksFineWithRequestTimeout() throws Exception {
    SagaResponse response = null;
    try {
      response = transport.with(address, normalResource, "GET", emptyMap());
    } catch (Exception e) {
      fail("unexpected exception throw: " + e);
    }
    assertThat(response.succeeded(), is(true));
  }

  @Test
  public void ensureSlowRequestFailsWithRequestTimeout() throws Exception {
    try {
      transport.with(address, slowResource, "GET", emptyMap());
      expectFailing(TransportFailedException.class);
    } catch (TransportFailedException e) {
      assertThat(SocketTimeoutException.class.isInstance(e.getCause()), is(true));
    }
  }

  @Configuration
  @ComponentScan
  static class Config {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }
}
