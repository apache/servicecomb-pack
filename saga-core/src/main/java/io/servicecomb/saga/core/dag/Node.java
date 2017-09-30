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

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Node<C, T> {
  private final long id;
  private final T value;
  private final Set<Node<C, T>> children = new HashSet<>();
  private final Set<Node<C, T>> parents = new HashSet<>();

  private final Set<Edge<C, T>> childrenEdges = new HashSet<>();
  private final Set<Edge<C, T>> parentEdges = new HashSet<>();

  public Node(long id, T value) {
    this.id = id;
    this.value = value;
  }

  long id() {
    return id;
  }

  public T value() {
    return value;
  }

  Set<Node<C, T>> parents() {
    return parents;
  }

  public Set<Node<C, T>> children() {
    return children;
  }

  public void addChild(Node<C, T> node) {
    children.add(node);
    node.parents.add(this);
  }

  public void addChildren(Collection<Node<C, T>> nodes) {
    children.addAll(nodes);
    nodes.forEach(node -> node.parents.add(this));
  }

  public void addChildEdge(Edge<C, T> edge) {
    childrenEdges.add(edge);
  }

  public void addParentEdge(Edge<C, T> edge) {
    parentEdges.add(edge);
  }

  public Set<Node<C, T>> children(C condition) {
    return childrenEdges.stream()
        .filter(edge -> edge.isSatisfied(condition))
        .map(Edge::target)
        .collect(Collectors.toSet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Node<?, ?> node = (Node<?, ?>) o;
    return id == node.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Node{" +
        "id=" + id +
        ", value=" + value +
        '}';
  }
}
