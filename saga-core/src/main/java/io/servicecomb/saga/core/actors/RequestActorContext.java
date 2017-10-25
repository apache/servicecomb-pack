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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.servicecomb.saga.core.RecoveryPolicy;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import akka.actor.ActorRef;

class RequestActorContext {
  private final RecoveryPolicy recoveryPolicy;
  private final Map<String, List<ActorRef>> parents;
  private final Map<String, List<ActorRef>> children;
  private final FromJsonFormat<Set<String>> childrenExtractor;

  RequestActorContext(
      RecoveryPolicy recoveryPolicy,
      FromJsonFormat<Set<String>> childrenExtractor) {

    this.recoveryPolicy = recoveryPolicy;
    this.children = new HashMap<>();
    this.parents = new HashMap<>();
    this.childrenExtractor = childrenExtractor;
  }

  void addChild(String requestId, ActorRef ref) {
    children.computeIfAbsent(requestId, k -> new ArrayList<>()).add(ref);
  }

  void addParent(String requestId, ActorRef ref) {
    parents.computeIfAbsent(requestId, k -> new ArrayList<>()).add(ref);
  }

  Collection<ActorRef> parentsOf(SagaRequest request) {
    return parents.get(request.id());
  }

  Collection<ActorRef> childrenOf(SagaRequest request) {
    return children.get(request.id());
  }

  void forAll(Consumer<ActorRef> consumer) {
    children.values()
        .stream()
        .flatMap(Collection::stream)
        .forEach(consumer);
  }

  Set<String> chosenChildren(SagaResponse response) {
    return childrenExtractor.fromJson(response.body());
  }

  RecoveryPolicy recoveryPolicy() {
    return recoveryPolicy;
  }
}
