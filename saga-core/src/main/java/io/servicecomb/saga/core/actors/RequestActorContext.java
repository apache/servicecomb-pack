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

package io.servicecomb.saga.core.actors;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import akka.actor.ActorRef;

class RequestActorContext {
  private final Map<String, ActorRef> actors;
  private final Map<String, List<ActorRef>> parents;
  private final Map<String, List<ActorRef>> children;
  private final FromJsonFormat<Set<String>> childrenExtractor;

  RequestActorContext(
      FromJsonFormat<Set<String>> childrenExtractor) {
    this.actors = new HashMap<>();
    this.children = new HashMap<>();
    this.parents = new HashMap<>();
    this.childrenExtractor = childrenExtractor;
  }

  void addActor(String id, ActorRef actorRef) {
    actors.put(id, actorRef);
  }

  void addChild(String requestId, ActorRef ref) {
    children.computeIfAbsent(requestId, k -> new ArrayList<>()).add(ref);
  }

  void addParent(String requestId, ActorRef ref) {
    parents.computeIfAbsent(requestId, k -> new ArrayList<>()).add(ref);
  }

  ActorRef actorOf(String id) {
    return actors.get(id);
  }

  Collection<ActorRef> parentsOf(SagaRequest request) {
    return parents.getOrDefault(request.id(), emptyList());
  }

  Collection<ActorRef> childrenOf(SagaRequest request) {
    return children.getOrDefault(request.id(), emptyList());
  }

  void forAll(Consumer<ActorRef> consumer) {
    actors.values()
        .stream()
        .forEach(consumer);
  }

  Set<String> chosenChildren(SagaResponse response) {
    return childrenExtractor.fromJson(response.body());
  }
}
