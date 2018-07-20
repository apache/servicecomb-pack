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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class NodeTest {


  private final Node<String> parent = new Node<>(0, "i don't care");
  private final Node<String> node1 = new Node<>(1, "i don't care");
  private final Node<String> node2 = new Node<>(2, "i don't care");
  private final Node<String> node3 = new Node<>(3, "i don't care");
  private final Node<String> node4 = new Node<>(4, "i don't care");
  private final Node<String> node5 = new Node<>(5, "i don't care");
  private final Node<String> node6 = new Node<>(6, "i don't care");

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
    parent.addChildren(asList(node1, node2));
    node1.addChildren(asList(node3, node4));
    node3.addChild(node5);
    node4.addChild(node5);
    node5.addChild(node6);
    node2.addChild(node6);
  }

  @Test
  public void nodeIsLinkedBidirectionally() {
    assertThat(parent.children(), containsInAnyOrder(node1, node2));
    assertThat(node1.parents(), contains(parent));
    assertThat(node1.children(), containsInAnyOrder(node3, node4));

    assertThat(node2.parents(), contains(parent));
    assertThat(node2.children(), contains(node6));

    assertThat(node3.parents(), contains(node1));
    assertThat(node3.children(), contains(node5));

    assertThat(node4.parents(), contains(node1));
    assertThat(node4.children(), contains(node5));

    assertThat(node5.parents(), containsInAnyOrder(node3, node4));
    assertThat(node5.children(), contains(node6));

    assertThat(node6.parents(), containsInAnyOrder(node2, node5));
    assertThat(node6.children().isEmpty(), is(true));
  }
}
