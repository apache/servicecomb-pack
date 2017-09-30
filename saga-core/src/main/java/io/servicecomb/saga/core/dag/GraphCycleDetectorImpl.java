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
public class GraphCycleDetectorImpl<C, T> implements GraphCycleDetector<C, T> {

  @Override
  public Set<Node<C, T>> cycleJoints(SingleLeafDirectedAcyclicGraph<C, T> graph) {
    Queue<Node<C, T>> orphanNodes = new LinkedList<>();
    Map<Node<C, T>, Set<Node<C, T>>> nodeParents = new HashMap<>();

    orphanNodes.add(graph.root());

    traverse(orphanNodes, nodeParents);

    return unvisitedNodes(nodeParents);
  }

  private void traverse(Queue<Node<C, T>> orphanNodes, Map<Node<C, T>, Set<Node<C, T>>> nodeParents) {
    while (!orphanNodes.isEmpty()) {
      Node<C, T> node = orphanNodes.poll();

      node.children().forEach(child -> {
        nodeParents.computeIfAbsent(child, n -> new HashSet<>(child.parents()))
            .remove(node);

        if (nodeParents.get(child).isEmpty()) {
          orphanNodes.add(child);
        }
      });
    }
  }

  private Set<Node<C, T>> unvisitedNodes(Map<Node<C, T>, Set<Node<C, T>>> nodeParents) {
    return nodeParents.entrySet()
        .parallelStream()
        .filter(parents -> !parents.getValue().isEmpty())
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }
}
