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

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class ByLevelTraveller<C, T> implements Traveller<C, T> {

  private final Collection<Node<C, T>> nodes;
  private final Collection<Node<C, T>> nodesBuffer;

  private final Queue<Node<C, T>> nodesWithoutParent = new LinkedList<>();
  private final Map<Long, Set<Node<C, T>>> nodeParents = new HashMap<>();
  private final TraversalDirection<C, T> traversalDirection;


  public ByLevelTraveller(SingleLeafDirectedAcyclicGraph<C, T> dag, TraversalDirection<C, T> traversalDirection) {
    this.nodes = new LinkedHashSet<>();
    this.nodesBuffer = new LinkedList<>();
    this.traversalDirection = traversalDirection;

    nodesWithoutParent.offer(traversalDirection.root(dag));
  }

  @Segment(name = "travelNext", category = "application", library = "kamon")
  @Override
  public void next() {
    nodes.addAll(nodesBuffer);
    nodesBuffer.clear();
    boolean buffered = false;

    while (!nodesWithoutParent.isEmpty() && !buffered) {
      Node<C, T> node = nodesWithoutParent.poll();
      nodes.add(node);

      for (Node<C, T> child : traversalDirection.children(node)) {
        nodeParents.computeIfAbsent(child.id(), id -> new HashSet<>(traversalDirection.parents(child)));
        nodeParents.get(child.id()).remove(node);

        if (nodeParents.get(child.id()).isEmpty()) {
          nodesWithoutParent.offer(child);
          nodesBuffer.add(child);
          buffered = true;
        }
      }
    }
  }

  @Override
  public boolean hasNext() {
    return !nodesWithoutParent.isEmpty();
  }

  @Override
  public Collection<Node<C, T>> nodes() {
    return nodes;
  }
}
