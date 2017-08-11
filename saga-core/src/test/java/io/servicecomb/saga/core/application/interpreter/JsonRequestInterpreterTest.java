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

package io.servicecomb.saga.core.application.interpreter;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.dag.ByLevelTraveller;
import io.servicecomb.saga.core.dag.FromRootTraversalDirection;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.core.dag.Traveller;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class JsonRequestInterpreterTest {

  private static final String requests = "[\n"
      + "  {\n"
      + "    \"id\": \"request-aaa\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"aaa\",\n"
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
      + "    \"serviceName\": \"bbb\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/bs\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"foo\": \"bs\"\n"
      + "        },\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"bar\\\": \\\"bs\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/bs\"\n"
      + "    }\n"
      + "  },\n"
      + "  {\n"
      + "    \"id\": \"request-ccc\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"ccc\",\n"
      + "    \"parents\": [\n"
      + "      \"request-aaa\",\n"
      + "      \"request-bbb\"\n"
      + "    ],\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/cs\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"foo\": \"cs\"\n"
      + "        },\n"
      + "        \"form\": {\n"
      + "          \"bar\": \"cs\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/cs\"\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  private static final String requestsWithDuplicateId = "[\n"
      + "  {\n"
      + "    \"id\": \"request-duplicate-id\",\n"
      + "    \"serviceName\": \"aaa\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/as\"\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/as\"\n"
      + "    }\n"
      + "  },\n"
      + "  {\n"
      + "    \"id\": \"request-duplicate-id\",\n"
      + "    \"serviceName\": \"bbb\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/bs\"\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/bs\"\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  private final JsonRequestInterpreter interpreter = new JsonRequestInterpreter(
      new SagaTaskFactory(null, null));

  @Test
  public void interpretsParallelRequests() {
    SingleLeafDirectedAcyclicGraph<SagaRequest> tasks = interpreter.interpret(requests);

    Traveller<SagaRequest> traveller = new ByLevelTraveller<>(tasks, new FromRootTraversalDirection<>());
    Collection<Node<SagaRequest>> nodes = traveller.nodes();

    traveller.next();
    assertThat(nodes, contains(taskWith("Saga", "nop", "/", "nop", "/")));
    nodes.clear();

    traveller.next();
    assertThat(nodes, contains(
        taskWith("aaa", "rest", "post", "/rest/as", "delete", "/rest/as",
            mapOf("form", mapOf("foo", "as")),
            mapOf("query", mapOf("bar", "as"))),
        taskWith("bbb", "rest", "post", "/rest/bs", "delete", "/rest/bs",
            mapOf("query", mapOf("foo", "bs"), "json", mapOf("body", "{ \"bar\": \"bs\" }")))
    ));
    nodes.clear();

    traveller.next();
    assertThat(nodes, contains(
        taskWith("ccc", "rest", "post", "/rest/cs", "delete", "/rest/cs",
            mapOf("query", mapOf("foo", "cs"), "form", mapOf("bar", "cs")))
    ));
    nodes.clear();

    traveller.next();
    assertThat(nodes, contains(taskWith("Saga", "nop", "/", "nop", "/")));
  }

  @Test
  public void blowsUpWhenJsonIsInvalid() {
    try {
      interpreter.interpret("invalid-json");
      fail(SagaException.class.getSimpleName() + " is expected, but none thrown");
    } catch (SagaException e) {
      assertThat(e.getMessage(), is("Failed to interpret JSON invalid-json"));
    }
  }

  @Test
  public void blowsUpWhenJsonContainsDuplicateRequestId() {
    try {
      interpreter.interpret(requestsWithDuplicateId);
      fail(SagaException.class.getSimpleName() + " is expected, but none thrown");
    } catch (SagaException e) {
      assertThat(e.getMessage(),
          is("Failed to interpret requests with duplicate request id: request-duplicate-id"));
    }
  }

  private Map<String, String> mapOf(String... pairs) {
    return new HashMap<String, String>() {{
      for (int i = 0; i < pairs.length; i += 2) {
        put(pairs[i], pairs[i + 1]);
      }
    }};
  }

  private Map<String, Map<String, String>> mapOf(
      String key1, Map<String, String> value1,
      String key2, Map<String, String> value2) {

    Map<String, Map<String, String>> map = new HashMap<>();
    map.put(key1, value1);
    map.put(key2, value2);
    return map;
  }

  private Map<String, Map<String, String>> mapOf(String key, Map<String, String> value) {
    return singletonMap(key, value);
  }

  private Matcher<? super Node<SagaRequest>> taskWith(
      String name,
      String transactionMethod,
      String transactionPath,
      String compensationMethod,
      String compensationPath) {
    return taskWith(name, "nop", transactionMethod, transactionPath, compensationMethod, compensationPath, emptyMap());
  }

  private Matcher<? super Node<SagaRequest>> taskWith(
      String name,
      String type,
      String transactionMethod,
      String transactionPath,
      String compensationMethod,
      String compensationPath,
      Map<String, Map<String, String>> transactionParams) {
    return taskWith(name,
        type,
        transactionMethod,
        transactionPath,
        compensationMethod,
        compensationPath,
        transactionParams,
        emptyMap());
  }

  private Matcher<? super Node<SagaRequest>> taskWith(
      String name,
      String type,
      String transactionMethod,
      String transactionPath,
      String compensationMethod,
      String compensationPath,
      Map<String, Map<String, String>> transactionParams,
      Map<String, Map<String, String>> compensationParams) {

    return new TypeSafeMatcher<Node<SagaRequest>>() {
      @Override
      protected boolean matchesSafely(Node<SagaRequest> node) {
        SagaRequest request = node.value();
        return request.serviceName().equals(name)
            && request.type().equals(type)
            && request.transaction().path().equals(transactionPath)
            && request.transaction().method().equals(transactionMethod)
            && request.compensation().path().equals(compensationPath)
            && request.compensation().method().equals(compensationMethod)
            && request.transaction().params().equals(transactionParams)
            && request.compensation().params().equals(compensationParams);
      }

      @Override
      protected void describeMismatchSafely(Node<SagaRequest> item, Description mismatchDescription) {
        SagaRequest request = item.value();
        mismatchDescription.appendText(
            "SagaRequest {name=" + request.serviceName()
                + ", type=" + request.type()
                + ", transaction=" + request.transaction().method() + ":" + request.transaction().path()
                + ", transaction params=" + request.transaction().params()
                + ", compensation=" + request.compensation().method() + ":" + request.compensation().path()
                + ", compensation params=" + request.compensation().params());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(
            "SagaRequest {name=" + name
                + ", type=" + type
                + ", transaction=" + transactionMethod + ":" + transactionPath
                + ", transaction params=" + transactionParams
                + ", compensation=" + compensationMethod + ":" + compensationPath
                + ", compensation params=" + compensationParams);
      }
    };
  }
}