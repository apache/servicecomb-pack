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
import java.util.function.Consumer;

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

  private void spitSatisfiedNodes(C condition, Collection<Node<C, T>> nodes) {
    do {
      Set<Node<C, T>> orphans = new LinkedHashSet<>();
      nodesBuffer.forEach(node -> {
        if (!traversalDirection.parents(node, condition).isEmpty()) {
          nodes.add(node);
        } else {
          nodesWithoutParent.remove(node);
          collectOrphans(node, orphans::add);
        }
      });
      nodesBuffer.clear();
      nodesBuffer.addAll(orphans);
    } while (!nodesBuffer.isEmpty());
  }

  @Segment(name = "travelNext", category = "application", library = "kamon")
  @Override
  public void next(C condition) {
    spitSatisfiedNodes(condition, nodes);

    while (!nodesWithoutParent.isEmpty() && nodesBuffer.isEmpty()) {
      Node<C, T> node = nodesWithoutParent.poll();
      nodes.add(node);

      collectOrphans(node, orphan -> {
        nodesWithoutParent.offer(orphan);
        nodesBuffer.add(orphan);
      });
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

  private void collectOrphans(Node<C, T> node, Consumer<Node<C, T>> orphanConsumer) {
    for (Node<C, T> child : traversalDirection.children(node)) {
      removeNodeFromChildParents(node, child);

      if (nodeParents.get(child.id()).isEmpty()) {
        orphanConsumer.accept(child);
      }
    }
  }

  private void removeNodeFromChildParents(Node<C, T> node, Node<C, T> child) {
    nodeParents
        .computeIfAbsent(child.id(), id -> new HashSet<>(traversalDirection.parents(child)))
        .remove(node);
  }
}
