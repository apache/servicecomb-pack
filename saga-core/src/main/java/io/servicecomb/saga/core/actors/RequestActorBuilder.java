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

import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_END_REQUEST;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;

import java.util.Map;
import java.util.Set;

import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class RequestActorBuilder {
  private final ActorSystem actorSystem;
  private final FromJsonFormat<Set<String>> childrenExtractor;

  RequestActorBuilder(
      ActorSystem actorSystem,
      FromJsonFormat<Set<String>> childrenExtractor) {

    this.actorSystem = actorSystem;
    this.childrenExtractor = childrenExtractor;
  }

  public ActorRef build(SagaRequest[] requests, Map<String, SagaTask> tasks, ActorRef completionCallback) {
    RequestActorContext context = new RequestActorContext(childrenExtractor);

    ActorRef rootActor = rootActor(context, tasks);
    ActorRef leafActor = leafActor(context, tasks);

    createRequestActors(requests, tasks, context);

    linkActorsById(rootActor, requests, context);
    addLeafToChildless(leafActor, requests, context);

    context.addParent(SAGA_START_REQUEST.id(), completionCallback);
    context.addChild(SAGA_END_REQUEST.id(), completionCallback);
    return rootActor;
  }

  private void linkActorsById(ActorRef rootActor, SagaRequest[] requests, RequestActorContext context) {
    for (SagaRequest request : requests) {
      if (isOrphan(request)) {
        context.addParent(request.id(), rootActor);
        context.addChild(SAGA_START_REQUEST.id(), context.actorOf(request.id()));
      } else {
        for (String parent : request.parents()) {
          context.addParent(request.id(), context.actorOf(parent));
          context.addChild(parent, context.actorOf(request.id()));
        }
      }
    }
  }

  private boolean isOrphan(SagaRequest request) {
    return request.parents().length == 0;
  }

  private void createRequestActors(SagaRequest[] requests, Map<String, SagaTask> tasks, RequestActorContext context) {
    for (SagaRequest request : requests) {
      Props props = RequestActor.props(context, tasks.get(request.task()), request);
      context.addActor(request.id(), actorSystem.actorOf(props));
    }
  }

  private void addLeafToChildless(ActorRef leafActor, SagaRequest[] requests, RequestActorContext context) {
    for (SagaRequest request : requests) {
      if (context.childrenOf(request).isEmpty()) {
        context.addParent(SAGA_END_REQUEST.id(), context.actorOf(request.id()));
        context.addChild(request.id(), leafActor);
      }
    }
  }

  private ActorRef rootActor(RequestActorContext context, Map<String, SagaTask> tasks) {
    Props root = RequestActor.props(context, tasks.get(SAGA_START_REQUEST.task()), SAGA_START_REQUEST);
    ActorRef actor = actorSystem.actorOf(root);
    context.addActor(SAGA_START_REQUEST.id(), actor);
    return actor;
  }

  private ActorRef leafActor(RequestActorContext context, Map<String, SagaTask> tasks) {
    Props leaf = RequestActor.props(context, tasks.get(SAGA_END_REQUEST.task()), SAGA_END_REQUEST);
    ActorRef actor = actorSystem.actorOf(leaf);
    context.addActor(SAGA_END_REQUEST.id(), actor);
    return actor;
  }
}
