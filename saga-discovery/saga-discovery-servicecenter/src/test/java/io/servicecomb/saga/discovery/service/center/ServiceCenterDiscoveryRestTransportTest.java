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

package io.servicecomb.saga.discovery.service.center;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static io.servicecomb.serviceregistry.client.LocalServiceRegistryClientImpl.LOCAL_REGISTRY_FILE_KEY;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.TransactionFailedException;
import io.servicecomb.saga.transports.RestTransport;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServiceCenterDiscoveryApplication.class, webEnvironment = RANDOM_PORT)
public class ServiceCenterDiscoveryRestTransportTest {

  private static final String usableResource = "/rest/usableResource";
  private static final String faultyResource = "/rest/faultyResource";
  private static final String usableResponse = "foo bar, hello world";
  private static final String json = "{\"hello\", \"world\"}";

  private final RestTransport transport = new ServiceCenterDiscoveryRestTransport();
  private final String serviceName = "saga-service";

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
  public void sendsGetRequestToDiscoveredService() {
    Map<String, Map<String, String>> requests = singletonMap("query", map("foo", "bar", "hello", "world"));
    SagaResponse response = transport.with(serviceName, usableResource, "get", requests);

    assertThat(response.succeeded(), is(true));
    assertThat(response.body(), allOf(
        containsString(usableResponse),
        containsString(String.valueOf(SC_OK))));
  }

  @Test
  public void putsRequestToDiscoveredService() {
    Map<String, Map<String, String>> requests = new HashMap<>();
    requests.put("query", singletonMap("foo", "bar"));
    requests.put("json", singletonMap("body", json));

    SagaResponse response = transport.with(serviceName, usableResource, "PUT", requests);

    assertThat(response.succeeded(), is(true));
    assertThat(response.body(), allOf(
        containsString(usableResponse + json),
        containsString(String.valueOf(SC_OK))));
  }

  @Test
  public void blowsUpWhenRemoteResponseIsNot2XX() {
    Map<String, Map<String, String>> requests = new HashMap<>();
    requests.put("query", singletonMap("foo", "bar"));
    requests.put("form", map("hello", "world"));

    try {
      transport.with(serviceName, faultyResource, "POST", requests);
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getMessage(), containsString("The remote service " + serviceName + " failed to serve"));
    }
  }

  @Test
  public void blowsUpWhenRemoteIsNotReachable() {
    try {
      transport.with("unknown-service", faultyResource, "DELETE", emptyMap());
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getMessage(), containsString("The remote service unknown-service failed to serve"));
    }
  }

  @Test
  public void blowsUpWhenMethodIsUnknown() {
    try {
      transport.with(serviceName, usableResource, "Blah", emptyMap());
      expectFailing(TransactionFailedException.class);
    } catch (TransactionFailedException e) {
      assertThat(e.getCause().getMessage(), is("No such method Blah"));
    }
  }

  private Map<String, String> map(String... pairs) {
    return new LinkedHashMap<String, String>() {{
      for (int i = 0; i < pairs.length; i += 2) {
        put(pairs[i], pairs[i + 1]);
      }
    }};
  }
}