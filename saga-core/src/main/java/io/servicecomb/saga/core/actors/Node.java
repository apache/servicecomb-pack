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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.servicecomb.saga.core.CompositeSagaResponse;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class Node extends AbstractLoggingActor {
  private final SagaRequest request;
  private final Map<String, List<ActorRef>> children;
  private final Map<String, SagaResponse> parentContexts;

  static Props props(SagaRequest request, Map<String, List<ActorRef>> children) {
    return Props.create(Node.class, () -> new Node(request, children));
  }

  public Node(SagaRequest request, Map<String, List<ActorRef>> children) {
    this.request = request;
    this.children = children;
    this.parentContexts = new HashMap<>(request.parents().length);
  }


  @Override
  public Receive createReceive() {
    return receiveBuilder().match(ResponseContext.class, parentContext -> {
      parentContexts.put(parentContext.request().id(), parentContext.response());
      if (parentContexts.size() == request.parents().length) {
        SagaResponse sagaResponse = request.transaction().send(request.serviceName(), responseOf(parentContexts));
        children.get(request.id()).forEach(actor -> actor.tell(new ResponseContext(request, sagaResponse), self()));
      }
    }).build();
  }

  private SagaResponse responseOf(Map<String, SagaResponse> responseContexts) {
    return responseContexts.size() > 1 ? new CompositeSagaResponse(responseContexts.values())
        : responseContexts.values().iterator().next();
  }
}
