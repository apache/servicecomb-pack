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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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

  private final String condition = "whatever it is";

  private final Predicate<String> predicate_p_1 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_p_2 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_1_3 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_1_4 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_3_5 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_4_5 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_2_6 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_5_6 = Mockito.mock(Predicate.class);

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
    parent.addChild(predicate_p_1, node1);
    parent.addChild(predicate_p_2, node2);
    node1.addChild(predicate_1_3, node3);
    node1.addChild(predicate_1_4, node4);
    node3.addChild(predicate_3_5, node5);
    node4.addChild(predicate_4_5, node5);
    node2.addChild(predicate_2_6, node6);
    node5.addChild(predicate_5_6, node6);
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
  public void relativesContainsSatisfiedOnesOnly() throws Exception {
    when(predicate_p_1.test(condition)).thenReturn(true);
    assertThat(parent.children(condition), contains(node1));
    assertThat(node1.parents(condition), contains(parent));
    assertThat(node2.parents(condition).isEmpty(), is(true));

    when(predicate_1_3.test(condition)).thenReturn(true);
    when(predicate_1_4.test(condition)).thenReturn(true);
    assertThat(node1.children(condition), contains(node3, node4));
    assertThat(node3.parents(condition), contains(node1));
    assertThat(node4.parents(condition), contains(node1));

    assertThat(node2.children(condition).isEmpty(), is(true));

    when(predicate_3_5.test(condition)).thenReturn(true);
    assertThat(node3.children(condition), contains(node5));

    when(predicate_4_5.test(condition)).thenReturn(true);
    assertThat(node4.children(condition), contains(node5));
    assertThat(node5.parents(condition), contains(node3, node4));

    when(predicate_5_6.test(condition)).thenReturn(true);
    assertThat(node5.children(condition), contains(node6));

    assertThat(node6.children(condition).isEmpty(), is(true));
    assertThat(node6.parents(condition), contains(node5));
  }
}