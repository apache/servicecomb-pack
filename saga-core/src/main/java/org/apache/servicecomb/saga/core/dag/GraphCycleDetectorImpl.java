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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cycle detection is based on topological sort with Kahn's algorithm.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Topological_sorting">Topological Sorting</a>
 */
public class GraphCycleDetectorImpl<T> implements GraphCycleDetector<T> {

  @Override
  public Set<Node<T>> cycleJoints(SingleLeafDirectedAcyclicGraph<T> graph) {
    Queue<Node<T>> orphanNodes = new LinkedList<>();
    Map<Node<T>, Set<Node<T>>> nodeParents = new HashMap<>();

    orphanNodes.add(graph.root());

    traverse(orphanNodes, nodeParents);

    return unvisitedNodes(nodeParents);
  }

  // This method is not thread safe
  private void traverse(Queue<Node<T>> orphanNodes, Map<Node<T>, Set<Node<T>>> nodeParents) {
    while (!orphanNodes.isEmpty()) {
      Node<T> node = orphanNodes.poll();

      for(Node<T> child : node.children()) {
        Set<Node<T>> parent = nodeParents.get(child);
        if (parent == null) {
          parent = new HashSet<>(child.parents());
          nodeParents.put(child, parent);
        }
        parent.remove(node);
        if (nodeParents.get(child).isEmpty()) {
          orphanNodes.add(child);
        }
      }
    }
  }

  private Set<Node<T>> unvisitedNodes(Map<Node<T>, Set<Node<T>>> nodeParents) {
    Set<Node<T>> result = new HashSet<>();
    for (Map.Entry<Node<T>, Set<Node<T>>> entry : nodeParents.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        result.add(entry.getKey());
      }
    }
    return result;
  }
}
