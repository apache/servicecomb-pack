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

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Node<T> {
  private final long id;
  private final T value;
  private final Set<Node<T>> children = new HashSet<>();
  private final Set<Node<T>> parents = new HashSet<>();

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

  Set<Node<T>> parents() {
    return parents;
  }

  public Set<Node<T>> children() {
    return children;
  }

  public void addChild(Node<T> node) {
    children.add(node);
    node.parents.add(this);
  }

  public void addChildren(Collection<Node<T>> nodes) {
    children.addAll(nodes);
    for (Node<T> node : nodes) {
      node.parents.add(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Node<?> node = (Node<?>) o;
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
