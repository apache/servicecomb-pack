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

package org.apache.servicecomb.saga.format;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.*;

import org.apache.servicecomb.saga.core.Operation;
import org.apache.servicecomb.saga.core.SagaException;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SuccessfulSagaResponse;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import org.apache.servicecomb.saga.transports.RestTransport;
import org.apache.servicecomb.saga.transports.TransportFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.seanyinx.github.unit.scaffolding.AssertUtils;

import org.apache.servicecomb.saga.core.SagaDefinition;

public class JacksonFromJsonFormatTest {

  private static final String requests = "{\n"
      + "  \"requests\": [\n"
      + "    {\n"
      + "      \"id\": \"request-aaa\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"aaa\",\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/rest/as\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"foo\": \"as\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"method\": \"delete\",\n"
      + "        \"path\": \"/rest/as\",\n"
      + "        \"params\": {\n"
      + "          \"query\": {\n"
      + "            \"bar\": \"as\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"fallback\": {\n"
      + "        \"type\": \"rest\",\n"
      + "        \"method\":\"put\",\n"
      + "        \"path\": \"/rest/as\"\n"
      + "      }\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": \"request-bbb\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"bbb\",\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/rest/bs\",\n"
      + "        \"params\": {\n"
      + "          \"query\": {\n"
      + "            \"foo\": \"bs\"\n"
      + "          },\n"
      + "          \"json\": {\n"
      + "            \"body\": \"{ \\\"bar\\\": \\\"bs\\\" }\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"retries\": 4,\n"
      + "        \"method\": \"delete\",\n"
      + "        \"path\": \"/rest/bs\"\n"
      + "      },\n"
      + "      \"fallback\": {\n"
      + "        \"type\": \"rest\",\n"
      + "        \"method\":\"put\",\n"
      + "        \"path\": \"/rest/bs\"\n"
      + "      }\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": \"request-ccc\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"ccc\",\n"
      + "      \"parents\": [\n"
      + "        \"request-aaa\",\n"
      + "        \"request-bbb\"\n"
      + "      ],\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/rest/cs\",\n"
      + "        \"params\": {\n"
      + "          \"query\": {\n"
      + "            \"foo\": \"cs\"\n"
      + "          },\n"
      + "          \"form\": {\n"
      + "            \"bar\": \"cs\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"retries\": 5,\n"
      + "        \"method\": \"delete\",\n"
      + "        \"path\": \"/rest/cs\"\n"
      + "      },\n"
      + "      \"fallback\": {\n"
      + "        \"type\": \"rest\",\n"
      + "        \"method\":\"put\",\n"
      + "        \"path\": \"/rest/cs\"\n"
      + "      }\n"
      + "    }\n"
      + "  ]\n"
      + "}\n";

  private final SagaResponse response11 = new SuccessfulSagaResponse(uniquify("response11"));
  private final SagaResponse response12 = new SuccessfulSagaResponse(uniquify("response12"));
  private final SagaResponse response13 = new SuccessfulSagaResponse(uniquify("response13"));
  private final SagaResponse response21 = new SuccessfulSagaResponse(uniquify("response21"));
  private final SagaResponse response22 = new SuccessfulSagaResponse(uniquify("response22"));
  private final SagaResponse response23 = new SuccessfulSagaResponse(uniquify("response23"));
  private final SagaResponse response31 = new SuccessfulSagaResponse(uniquify("response31"));
  private final SagaResponse response32 = new SuccessfulSagaResponse(uniquify("response32"));
  private final SagaResponse response33 = new SuccessfulSagaResponse(uniquify("response33"));

  private final RestTransport restTransport = Mockito.mock(RestTransport.class);
  private final TransportFactory transportFactory = Mockito.mock(TransportFactory.class);
  private final FromJsonFormat<SagaDefinition> format = new JacksonFromJsonFormat(transportFactory);

  private final Function<SagaRequest, String> getRequestId = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.id();
    }
  };

  private final Function<SagaRequest, String> getRequestServiceName = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.serviceName();
    }
  };

  private final Function<SagaRequest, String> getRequestType = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.type();
    }
  };

  private final Function<SagaRequest, Integer> getCompensationRetries = new Function<SagaRequest, Integer>() {
    @Override
    public Integer apply(SagaRequest sagaRequest) {
      return sagaRequest.compensation().retries();
    }
  };

  private final Function<SagaRequest, String> getFallbackType = new Function<SagaRequest, String>() {
    @Override
    public String apply(SagaRequest sagaRequest) {
      return sagaRequest.fallback().type();
    }
  };

  @Before
  public void setUp() throws Exception {
    when(transportFactory.restTransport()).thenReturn(restTransport);

    when(restTransport.with("aaa", "/rest/as", "post", singletonMap("form", singletonMap("foo", "as"))))
        .thenReturn(response11);
    when(restTransport.with("aaa", "/rest/as", "delete", singletonMap("query", singletonMap("bar", "as"))))
        .thenReturn(response12);
    when(restTransport.with("aaa", "/rest/as", "put", Collections.<String, Map<String, String>>emptyMap()))
        .thenReturn(response13);

    when(restTransport
        .with("bbb", "/rest/bs", "post",
            mapOf("query", singletonMap("foo", "bs"), "json", singletonMap("body", "{ \"bar\": \"bs\" }"))))
        .thenReturn(response21);
    when(restTransport.with("bbb", "/rest/bs", "delete", Collections.<String, Map<String, String>>emptyMap()))
        .thenReturn(response22);
    when(restTransport.with("bbb", "/rest/bs", "put", Collections.<String, Map<String, String>>emptyMap()))
        .thenReturn(response23);

    when(restTransport
        .with("ccc", "/rest/cs", "post", mapOf("query", singletonMap("foo", "cs"), "form", singletonMap("bar", "cs"))))
        .thenReturn(response31);
    when(restTransport.with("ccc", "/rest/cs", "delete", Collections.<String, Map<String, String>>emptyMap()))
        .thenReturn(response32);
    when(restTransport.with("ccc", "/rest/cs", "put", Collections.<String, Map<String, String>>emptyMap()))
        .thenReturn(response33);
  }

  @Test
  public void addTransportToDeserializedRequests() throws IOException {
    SagaRequest[] sagaRequests = format.fromJson(requests).requests();

    assertThat(collect(sagaRequests, getRequestId), contains("request-aaa", "request-bbb", "request-ccc"));
    assertThat(collect(sagaRequests, getRequestServiceName), contains("aaa", "bbb", "ccc"));
    assertThat(collect(sagaRequests, getRequestType), Matchers
        .contains(Operation.TYPE_REST, Operation.TYPE_REST, Operation.TYPE_REST));
    assertThat(collect(sagaRequests, getCompensationRetries), contains(3, 4, 5));
    assertThat(collect(sagaRequests, getFallbackType), Matchers
        .contains(Operation.TYPE_REST, Operation.TYPE_REST, Operation.TYPE_REST));

    SagaResponse response = sagaRequests[0].transaction().send("aaa");
    assertThat(response, is(response11));

    response = sagaRequests[0].compensation().send("aaa");
    assertThat(response, is(response12));

    response = sagaRequests[0].fallback().send("aaa");
    assertThat(response, is(response13));
    assertThat(sagaRequests[0].parents().length, is(0));

    response = sagaRequests[1].transaction().send("bbb");
    assertThat(response, is(response21));

    response = sagaRequests[1].compensation().send("bbb");
    assertThat(response, is(response22));

    response = sagaRequests[1].fallback().send("bbb");
    assertThat(response, is(response23));
    assertThat(sagaRequests[1].parents().length, is(0));

    response = sagaRequests[2].transaction().send("ccc");
    assertThat(response, is(response31));

    response = sagaRequests[2].compensation().send("ccc");
    assertThat(response, is(response32));

    response = sagaRequests[2].fallback().send("ccc");
    assertThat(response, is(response33));
    assertArrayEquals(new String[] {"request-aaa", "request-bbb"}, sagaRequests[2].parents());
  }

  @Test
  public void blowsUpWhenJsonIsInvalid() throws IOException {
    String invalidRequest = "invalid-json";

    try {
      format.fromJson(invalidRequest);
      AssertUtils.expectFailing(SagaException.class);
    } catch (SagaException e) {
      assertThat(e.getMessage(), is("Failed to interpret JSON invalid-json"));
    }
  }

  private <T> Collection<T> collect(SagaRequest[] requests, Function<SagaRequest, T> mapper) {
    List<T> result = new LinkedList<T>();
    for (SagaRequest request : requests) {
      result.add(mapper.apply(request));
    }
    return result;
  }

  private Map<String, Map<String, String>> mapOf(
      String key1,
      Map<String, String> value1,
      String key2,
      Map<String, String> value2) {

    Map<String, Map<String, String>> map = new HashMap<>();
    map.put(key1, value1);
    map.put(key2, value2);
    return map;
  }

  private interface Function<T, R> {
    R apply(T t);
  }
}
