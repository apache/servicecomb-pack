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

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_END_REQUEST;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;
import static io.servicecomb.saga.core.SagaRequest.TYPE_REST;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.servicecomb.saga.core.CompensationImpl;
import io.servicecomb.saga.core.SagaException;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaRequestImpl;
import io.servicecomb.saga.core.TransactionImpl;
import io.servicecomb.saga.core.dag.ByLevelTraveller;
import io.servicecomb.saga.core.dag.FromRootTraversalDirection;
import io.servicecomb.saga.core.dag.GraphCycleDetector;
import io.servicecomb.saga.core.dag.Node;
import io.servicecomb.saga.core.dag.SingleLeafDirectedAcyclicGraph;
import io.servicecomb.saga.core.dag.Traveller;
import java.util.Collection;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
public class JsonRequestInterpreterTest {

  private final SagaRequest request1 = new SagaRequestImpl(
      "request-aaa",
      "aaa",
      TYPE_REST,
      new TransactionImpl("/rest/as", "post", emptyMap()),
      new CompensationImpl("/rest/as","delete", emptyMap())
  );

  private final SagaRequest request2 = new SagaRequestImpl(
      "request-bbb",
      "bbb",
      TYPE_REST,
      new TransactionImpl("/rest/bs", "post", emptyMap()),
      new CompensationImpl("/rest/bs","delete", emptyMap())
  );

  private final SagaRequest request3 = new SagaRequestImpl(
      "request-ccc",
      "ccc",
      TYPE_REST,
      new TransactionImpl("/rest/cs", "post", emptyMap()),
      new CompensationImpl("/rest/cs","delete", emptyMap()),
      new String[]{"request-aaa", "request-bbb"}
  );
  private final SagaRequest[] requests = {request1, request2, request3};

  private final SagaRequest duplicateRequest = new SagaRequestImpl(
      "request-duplicate-id",
      "xxx",
      TYPE_REST,
      new TransactionImpl("/rest/xs", "post", emptyMap()),
      new CompensationImpl("/rest/xs","delete", emptyMap())
  );
  private final SagaRequest[] duplicateRequests = {duplicateRequest, duplicateRequest};

  private final GraphCycleDetector<SagaRequest> detector = Mockito.mock(GraphCycleDetector.class);
  private final JsonRequestInterpreter interpreter = new JsonRequestInterpreter(detector);

  @Before
  public void setUp() throws Exception {
    when(detector.cycleJoints(any())).thenReturn(emptySet());
  }

  @Test
  public void interpretsParallelRequests() {
    SingleLeafDirectedAcyclicGraph<SagaRequest> tasks = interpreter.interpret(requests);

    Traveller<SagaRequest> traveller = new ByLevelTraveller<>(tasks, new FromRootTraversalDirection<>());
    Collection<Node<SagaRequest>> nodes = traveller.nodes();

    traveller.next();
    assertThat(requestsOf(nodes), contains(SAGA_START_REQUEST));
    nodes.clear();

    traveller.next();
    assertThat(requestsOf(nodes), contains(request1, request2));
    nodes.clear();

    traveller.next();
    assertThat(requestsOf(nodes), contains(request3));
    nodes.clear();

    traveller.next();
    assertThat(requestsOf(nodes), contains(SAGA_END_REQUEST));
  }

  @Test
  public void blowsUpWhenJsonContainsDuplicateRequestId() {
    try {
      interpreter.interpret(duplicateRequests);
      fail(SagaException.class.getSimpleName() + " is expected, but none thrown");
    } catch (SagaException e) {
      assertThat(e.getMessage(),
          is("Failed to interpret requests with duplicate request id: request-duplicate-id"));
    }
  }

  @Test
  public void blowsUpWhenGraphContainsCycle() {
    reset(detector);
    when(detector.cycleJoints(any())).thenReturn(singleton(new Node<>(0L, null)));

    try {
      interpreter.interpret(requests);
      expectFailing(SagaException.class);
    } catch (SagaException e) {
      assertThat(e.getMessage(), startsWith("Cycle detected in the request graph at nodes "));
    }
  }

  private Collection<SagaRequest> requestsOf(Collection<Node<SagaRequest>> nodes) {
    return nodes.stream()
        .map(Node::value)
        .collect(Collectors.toList());
  }
}