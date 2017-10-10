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

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.seanyinx.github.unit.scaffolding.Randomness;

@SuppressWarnings("unchecked")
public class DirectedAcyclicGraphTraversalTest {

  private final Node<String, String> root = new Node<>(0, "root");
  private final Node<String, String> node1 = new Node<>(1, "node1");
  private final Node<String, String> node2 = new Node<>(2, "node2");
  private final Node<String, String> node3 = new Node<>(3, "node3");
  private final Node<String, String> node4 = new Node<>(4, "node4");
  private final Node<String, String> node5 = new Node<>(5, "node5");
  private final Node<String, String> leaf = new Node<>(6, "leaf");

  private final String condition = Randomness.uniquify("condition");

  private final Predicate<String> predicate_p_1 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_p_2 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_1_3 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_1_4 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_3_5 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_4_5 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_2_6 = Mockito.mock(Predicate.class);
  private final Predicate<String> predicate_5_6 = Mockito.mock(Predicate.class);

  private final SingleLeafDirectedAcyclicGraph<String, String> dag = new SingleLeafDirectedAcyclicGraph<>(root, leaf);

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
    root.addChild(predicate_p_1, node1);
    root.addChild(predicate_p_2, node2);
    node1.addChild(predicate_1_3, node3);
    node1.addChild(predicate_1_4, node4);
    node3.addChild(predicate_3_5, node5);
    node4.addChild(predicate_4_5, node5);
    node2.addChild(predicate_2_6, leaf);
    node5.addChild(predicate_5_6, leaf);
  }

  @Test
  public void traverseGraphOneLevelPerStepFromRoot() {
    markAllSatisfied();
    Traveller<String, String> traveller = new ByLevelTraveller<>(dag, new FromRootTraversalDirection<>());

    Collection<Node<String, String>> nodes = traveller.nodes();

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2, node3, node4));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2, node3, node4, node5));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2, node3, node4, node5, leaf));

    assertThat(traveller.hasNext(), is(false));
  }

  @Test
  public void traverseOnlySatisfiedChildrenFromRoot() {
    Traveller<String, String> traveller = new ByLevelTraveller<>(dag, new FromRootTraversalDirection<>());

    Collection<Node<String, String>> nodes = traveller.nodes();

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root));

    when(predicate_p_1.test(condition)).thenReturn(true);
    when(predicate_p_2.test(condition)).thenReturn(true);
    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2));

    when(predicate_1_4.test(condition)).thenReturn(true);
    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2, node4));

    when(predicate_2_6.test(condition)).thenReturn(true);
    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(root, node1, node2, node4, leaf));

    assertThat(traveller.hasNext(), is(false));
  }

  @Test
  public void traverseGraphOneLevelPerStepFromLeaf() {
    markAllSatisfied();
    Traveller<String, String> traveller = new ByLevelTraveller<>(dag, new FromLeafTraversalDirection<>());

    Collection<Node<String, String>> nodes = traveller.nodes();

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf, node2, node5));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf, node2, node5, node3, node4));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf, node2, node5, node3, node4, node1));

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf, node2, node5, node3, node4, node1, root));

    assertThat(traveller.hasNext(), is(false));
  }

  @Test
  public void traverseOnlySatisfiedNodesFromLeaf() {
    Traveller<String, String> traveller = new ByLevelTraveller<>(dag, new FromLeafTraversalDirection<>());

    Collection<Node<String, String>> nodes = traveller.nodes();

    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf));

    when(predicate_2_6.test(condition)).thenReturn(true);
    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf, node2));

    when(predicate_p_2.test(condition)).thenReturn(true);
    assertThat(traveller.hasNext(), is(true));
    traveller.next(condition);
    assertThat(nodes, contains(leaf, node2, root));

    assertThat(traveller.hasNext(), is(false));
  }

  private void markAllSatisfied() {
    when(predicate_p_1.test(condition)).thenReturn(true);
    when(predicate_p_2.test(condition)).thenReturn(true);
    when(predicate_1_3.test(condition)).thenReturn(true);
    when(predicate_1_4.test(condition)).thenReturn(true);
    when(predicate_3_5.test(condition)).thenReturn(true);
    when(predicate_4_5.test(condition)).thenReturn(true);
    when(predicate_2_6.test(condition)).thenReturn(true);
    when(predicate_5_6.test(condition)).thenReturn(true);
  }
}