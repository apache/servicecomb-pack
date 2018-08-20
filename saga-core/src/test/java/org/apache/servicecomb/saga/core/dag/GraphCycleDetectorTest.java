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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Set;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class GraphCycleDetectorTest {

  private final Node<String> root = node(0);
  private final Node<String> leaf = node(Long.MAX_VALUE);

  private final Node<String> node1 = node(1);
  private final Node<String> node2 = node(2);
  private final Node<String> node3 = node(3);

  private final SingleLeafDirectedAcyclicGraph<String> graph = new SingleLeafDirectedAcyclicGraph<>(root, leaf);
  private final GraphCycleDetector<String> detector = new GraphCycleDetectorImpl<>();

  @Before
  public void setUp() throws Exception {
    root.addChild(node1);

    node2.addChild(leaf);
    node3.addChild(leaf);
  }

  @Test
  public void emptyNodesWhenNoCycleInGraph() {
    node1.addChild(node2);
    node1.addChild(node3);

    Set<Node<String>> nodes = detector.cycleJoints(graph);
    assertThat(nodes.isEmpty(), is(true));
  }

  @Test
  public void nonEmptyNodesIfGraphContainsCycle() {
    node1.addChild(node2);
    node2.addChild(node3);
    node3.addChild(node1);

    Set<Node<String>> nodes = detector.cycleJoints(graph);
   
    assertThat(nodes, contains(node1));
  }

  private Node<String> node(long id) {
    return new Node<>(id, "value " + id);
  }
}
