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

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.dag.ByLevelTraveller;
import io.servicecomb.saga.core.dag.FromRootTraversalDirection;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.core.dag.Traveller;
import java.util.Collection;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.mockito.Mockito;

public class JsonRequestInterpreterTest {

  private static final String requests = "[\n"
      + "  {\n"
      + "    \"id\": \"request-aaa\",\n"
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
      + "    \"id\": \"request-bbb\",\n"
      + "    \"serviceName\": \"bbb\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/bs\"\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/bs\"\n"
      + "    }\n"
      + "  },\n"
      + "  {\n"
      + "    \"id\": \"request-ccc\",\n"
      + "    \"serviceName\": \"ccc\",\n"
      + "    \"parents\": [\"request-aaa\", \"request-bbb\"], \n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/cs\"\n"
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

  private final SagaTask sagaStartCommand = Mockito.mock(SagaTask.class, "sagaStartCommand");
  private final SagaTask taskCommand = Mockito.mock(SagaTask.class, "taskCommand");
  private final SagaTask sagaEndCommand = Mockito.mock(SagaTask.class, "sagaEndCommand");

  private final JsonRequestInterpreter interpreter = new JsonRequestInterpreter(
      new SagaTaskFactory(sagaStartCommand, taskCommand, sagaEndCommand));

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
        taskWith("aaa", "post", "/rest/as", "delete", "/rest/as"),
        taskWith("bbb", "post", "/rest/bs", "delete", "/rest/bs")
    ));
    nodes.clear();

    traveller.next();
    assertThat(nodes, contains(
        taskWith("ccc", "post", "/rest/cs", "delete", "/rest/cs")
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

  private Matcher<? super Node<SagaRequest>> taskWith(
      String name,
      String transactionMethod,
      String transactionPath,
      String compensationMethod,
      String compensationPath) {

    return new TypeSafeMatcher<Node<SagaRequest>>() {
      @Override
      protected boolean matchesSafely(Node<SagaRequest> node) {
        SagaRequest request = node.value();
        return request.serviceName().equals(name)
            && request.transaction().path().equals(transactionPath)
            && request.transaction().method().equals(transactionMethod)
            && request.compensation().path().equals(compensationPath)
            && request.compensation().method().equals(compensationMethod);
      }

      @Override
      protected void describeMismatchSafely(Node<SagaRequest> item, Description mismatchDescription) {
        SagaRequest request = item.value();
        mismatchDescription.appendText(
            "SagaRequest {name=" + request.serviceName()
                + ", transaction=" + request.transaction().method() + ":" + request.transaction().path()
                + ", compensation=" + request.compensation().method() + ":" + request.compensation().path());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(
            "SagaRequest {name=" + name
                + ", transaction=" + transactionMethod + ":" + transactionPath
                + ", compensation=" + compensationMethod + ":" + compensationPath);
      }
    };
  }
}