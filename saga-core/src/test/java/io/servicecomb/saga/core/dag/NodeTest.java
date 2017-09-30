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

package io.servicecomb.saga.core.dag;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class NodeTest {


  private final String value = "i don't care";

  private final Node<String, String> parent = new Node<>(0, value);
  private final Node<String, String> node1 = new Node<>(1, value);
  private final Node<String, String> node2 = new Node<>(2, value);
  private final Node<String, String> node3 = new Node<>(3, value);
  private final Node<String, String> node4 = new Node<>(4, value);
  private final Node<String, String> node5 = new Node<>(5, value);
  private final Node<String, String> node6 = new Node<>(6, value);

  private final String condition = "";

  private boolean satisfied_p_1;
  private final Edge<String, String> edge1 = new Edge<>(any -> satisfied_p_1, parent, node1);

  private boolean satisfied_p_2;
  private final Edge<String, String> edge2 = new Edge<>(any -> satisfied_p_2, parent, node2);

  private boolean satisfied_1_3;
  private final Edge<String, String> edge3 = new Edge<>(any -> satisfied_1_3, node1, node3);

  private boolean satisfied_1_4;
  private final Edge<String, String> edge4 = new Edge<>(any -> satisfied_1_4, node1, node4);

  private boolean satisfied_3_5;
  private final Edge<String, String> edge51 = new Edge<>(any -> satisfied_3_5, node3, node5);

  private boolean satisfied_4_5;
  private final Edge<String, String> edge52 = new Edge<>(any -> satisfied_4_5, node4, node5);

  private boolean satisfied_2_6;
  private final Edge<String, String> edge61 = new Edge<>(any -> satisfied_2_6, node2, node6);

  private boolean satisfied_5_6;
  private final Edge<String, String> edge62 = new Edge<>(any -> satisfied_5_6, node5, node6);

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

  @Test
  public void childrenContainsSatisfiedOnesOnly() throws Exception {
    satisfied_p_1 = true;
    assertThat(parent.children(condition), contains(node1));

    satisfied_1_3 = true;
    satisfied_1_4 = true;
    assertThat(node1.children(condition), contains(node3, node4));

    assertThat(node2.children(condition).isEmpty(), is(true));

    assertThat(node3.children(condition).isEmpty(), is(true));

    satisfied_4_5 = true;
    assertThat(node4.children(condition), contains(node5));

    satisfied_5_6 = true;
    assertThat(node5.children(condition), contains(node6));

    assertThat(node6.children(condition).isEmpty(), is(true));
  }
}