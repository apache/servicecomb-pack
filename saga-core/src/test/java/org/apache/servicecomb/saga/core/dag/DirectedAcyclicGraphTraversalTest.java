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

import static java.util.Arrays.asList;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import org.junit.Before;
import org.junit.Test;

public class DirectedAcyclicGraphTraversalTest {

  private final Node<String> root = new Node<>(0, "i don't care");
  private final Node<String> node1 = new Node<>(1, "i don't care");
  private final Node<String> node2 = new Node<>(2, "i don't care");
  private final Node<String> node3 = new Node<>(3, "i don't care");
  private final Node<String> node4 = new Node<>(4, "i don't care");
  private final Node<String> node5 = new Node<>(5, "i don't care");
  private final Node<String> leaf = new Node<>(6, "i don't care");

  private final SingleLeafDirectedAcyclicGraph<String> dag = new SingleLeafDirectedAcyclicGraph<>(root, leaf);

  //        0
  //       / \
  //      1   \
  //     / \   \
  //    3   4   2
  //     \ /   /
  //      5   /
  //       \ /
  //        6
  @Before
  public void setUp() throws Exception {
    root.addChildren(asList(node1, node2));
    node1.addChildren(asList(node3, node4));
    node3.addChild(node5);
    node4.addChild(node5);
    node5.addChild(leaf);
    node2.addChild(leaf);
  }

  @Test
  public void traverseGraphOneLevelPerStepFromRoot() {
    Traveller<String> traveller = new ByLevelTraveller<>(dag, new FromRootTraversalDirection<String>());

    Collection<Node<String>> nodes = traveller.nodes();

    traveller.next();
    assertThat(nodes, contains(root));

    traveller.next();
    assertThat(nodes, contains(root, node1, node2));

    traveller.next();
    assertThat(nodes, contains(root, node1, node2, node3, node4));

    traveller.next();
    assertThat(nodes, contains(root, node1, node2, node3, node4, node5));

    traveller.next();
    assertThat(nodes, contains(root, node1, node2, node3, node4, node5, leaf));
  }

  @Test
  public void traverseGraphOneLevelPerStepFromLeaf() {
    Traveller<String> traveller = new ByLevelTraveller<>(dag, new FromLeafTraversalDirection<String>());

    Collection<Node<String>> nodes = traveller.nodes();

    traveller.next();
    assertThat(nodes, contains(leaf));

    traveller.next();
    assertThat(nodes, contains(leaf, node2, node5));

    traveller.next();
    assertThat(nodes, contains(leaf, node2, node5, node3, node4));

    traveller.next();
    assertThat(nodes, contains(leaf, node2, node5, node3, node4, node1));

    traveller.next();
    assertThat(nodes, contains(leaf, node2, node5, node3, node4, node1, root));
  }
}
