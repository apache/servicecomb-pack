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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Traveller<T> {

  private final Collection<Node<T>> nodes;
  private final Collection<Node<T>> nodesBuffer;

  private final Queue<Node<T>> nodesWithoutParent = new LinkedList<>();
  private final Map<Integer, Set<Node<T>>> nodeParents = new HashMap<>();


  public Traveller(DirectedAcyclicGraph<T> dag) {
    this.nodes = new LinkedHashSet<>();
    this.nodesBuffer = new LinkedList<>();

    nodesWithoutParent.offer(dag.root());
  }

  public void next() {
    nodes.addAll(nodesBuffer);
    nodesBuffer.clear();
    boolean buffered = false;

    while (!nodesWithoutParent.isEmpty() && !buffered) {
      Node<T> node = nodesWithoutParent.poll();
      nodes.add(node);

      for (Node<T> child : node.children()) {
        nodeParents.computeIfAbsent(child.id(), id -> new HashSet<>(child.parents()));
        nodeParents.get(child.id()).remove(node);

        if (nodeParents.get(child.id()).isEmpty()) {
          nodesWithoutParent.offer(child);
          nodesBuffer.add(child);
          buffered = true;
        }
      }
    }
  }

  public Collection<Node<T>> nodes() {
    return nodes;
  }
}
