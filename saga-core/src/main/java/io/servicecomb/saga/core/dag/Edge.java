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

import java.util.function.Predicate;

public class Edge<C, T> {
  private final Predicate<C> predicate;
  private final Node<C, T> parent;
  private final Node<C, T> child;

  public Edge(Predicate<C> predicate, Node<C, T> parent, Node<C, T> child) {
    this.predicate = predicate;
    this.parent = parent;
    this.child = child;

    parent.addChildEdge(this);
    child.addParentEdge(this);
  }

  Node<C, T> source() {
    return parent;
  }

  Node<C, T> target() {
    return child;
  }

  boolean isSatisfied(C condition) {
    return predicate.test(condition);
  }
}
