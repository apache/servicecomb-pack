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

package org.apache.servicecomb.saga.core.dag;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.apache.servicecomb.saga.core.Operation.TYPE_REST;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.servicecomb.saga.core.NoOpSagaRequest;
import org.apache.servicecomb.saga.core.SagaException;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaRequestImpl;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.servicecomb.saga.core.CompensationImpl;
import org.apache.servicecomb.saga.core.TransactionImpl;

@SuppressWarnings("unchecked")
public class GraphBuilderTest {

  public static final Map<String, Map<String, String>> EMPTY_MAP = Collections.<String, Map<String, String>>emptyMap();

  private final SagaRequest request1 = new SagaRequestImpl(
      "request-aaa",
      "aaa",
      TYPE_REST,
      new TransactionImpl("/rest/as", "post", EMPTY_MAP),
      new CompensationImpl("/rest/as","delete", EMPTY_MAP)
  );

  private final SagaRequest request2 = new SagaRequestImpl(
      "request-bbb",
      "bbb",
      TYPE_REST,
      new TransactionImpl("/rest/bs", "post", EMPTY_MAP),
      new CompensationImpl("/rest/bs","delete", EMPTY_MAP)
  );

  private final SagaRequest request3 = new SagaRequestImpl(
      "request-ccc",
      "ccc",
      TYPE_REST,
      new TransactionImpl("/rest/cs", "post", EMPTY_MAP),
      new CompensationImpl("/rest/cs","delete", EMPTY_MAP),
      null,
      new String[]{"request-aaa", "request-bbb"}
  );
  private final SagaRequest[] requests = {request1, request2, request3};

  private final SagaRequest duplicateRequest = new SagaRequestImpl(
      "request-duplicate-id",
      "xxx",
      TYPE_REST,
      new TransactionImpl("/rest/xs", "post", EMPTY_MAP),
      new CompensationImpl("/rest/xs","delete", EMPTY_MAP)
  );
  private final SagaRequest[] duplicateRequests = {duplicateRequest, duplicateRequest};

  private final GraphCycleDetector<SagaRequest> detector = Mockito.mock(GraphCycleDetector.class);
  private final GraphBuilder graphBuilder = new GraphBuilder(detector);

  @Before
  public void setUp() throws Exception {
    when(detector.cycleJoints((SingleLeafDirectedAcyclicGraph<SagaRequest>)any())).thenReturn((Set<Node<SagaRequest>>) Collections.EMPTY_SET);
  }

  @Test
  public void buildsGraphOfParallelRequests() {
    SingleLeafDirectedAcyclicGraph<SagaRequest> tasks = graphBuilder.build(requests);

    Traveller<SagaRequest> traveller = new ByLevelTraveller<>(tasks, new FromRootTraversalDirection<SagaRequest>());
    Collection<Node<SagaRequest>> nodes = traveller.nodes();

    traveller.next();
    assertThat(requestsOf(nodes), IsIterableContainingInOrder.contains(NoOpSagaRequest.SAGA_START_REQUEST));
    nodes.clear();

    traveller.next();
    assertThat(requestsOf(nodes), contains(request1, request2));
    nodes.clear();

    traveller.next();
    assertThat(requestsOf(nodes), contains(request3));
    nodes.clear();

    traveller.next();
    assertThat(requestsOf(nodes), IsIterableContainingInOrder.contains(NoOpSagaRequest.SAGA_END_REQUEST));
  }

  @Test
  public void blowsUpWhenJsonContainsDuplicateRequestId() {
    try {
      graphBuilder.build(duplicateRequests);
      fail(SagaException.class.getSimpleName() + " is expected, but none thrown");
    } catch (SagaException e) {
      assertThat(e.getMessage(),
          is("Failed to interpret requests with duplicate request id: request-duplicate-id"));
    }
  }

  @Test
  public void blowsUpWhenGraphContainsCycle() {
    reset(detector);
    when(detector.cycleJoints((SingleLeafDirectedAcyclicGraph<SagaRequest>) any())).thenReturn(singleton(new Node<SagaRequest>(0L, null)));

    try {
      graphBuilder.build(requests);
      expectFailing(SagaException.class);
    } catch (SagaException e) {
      assertThat(e.getMessage(), startsWith("Cycle detected in the request graph at nodes "));
    }
  }

  private Collection<SagaRequest> requestsOf(Collection<Node<SagaRequest>> nodes) {
    List<SagaRequest> result = new ArrayList<>();
    for(Node<SagaRequest> node: nodes) {
      result.add(node.value());
    }
    return result;
  }
}
