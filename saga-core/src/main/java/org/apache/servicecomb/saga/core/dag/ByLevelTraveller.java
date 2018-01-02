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
public class ByLevelTraveller<T> implements Traveller<T> {

  private final Collection<Node<T>> nodes;
  private final Collection<Node<T>> nodesBuffer;

  private final Queue<Node<T>> nodesWithoutParent = new LinkedList<>();
  private final Map<Long, Set<Node<T>>> nodeParents = new HashMap<>();
  private final TraversalDirection<T> traversalDirection;


  public ByLevelTraveller(SingleLeafDirectedAcyclicGraph<T> dag, TraversalDirection<T> traversalDirection) {
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
      Node<T> node = nodesWithoutParent.poll();
      nodes.add(node);

      for (Node<T> child : traversalDirection.children(node)) {
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
  public Collection<Node<T>> nodes() {
    return nodes;
  }
}
