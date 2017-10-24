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

import static io.servicecomb.saga.core.actors.Node.Messages.MESSAGE_ABORT;
import static io.servicecomb.saga.core.actors.Node.Messages.MESSAGE_COMPENSATE;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.servicecomb.saga.core.CompositeSagaResponse;
import io.servicecomb.saga.core.RecoveryPolicy;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.TransactionFailedException;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class Node extends AbstractLoggingActor {
  private final RecoveryPolicy recoveryPolicy;
  private final SagaTask task;
  private final SagaRequest request;

  private final Map<String, List<ActorRef>> parents;
  private final Map<String, List<ActorRef>> children;

  private final List<SagaResponse> parentResponses;
  private final List<ActorRef> compensatedChildren;
  private final Set<String> pendingParents;

  private final Receive transacted;
  private final Receive aborted;

  static Props props(
      RecoveryPolicy recoveryPolicy,
      SagaTask task,
      SagaRequest request,
      Map<String, List<ActorRef>> parents,
      Map<String, List<ActorRef>> children) {
    return Props.create(Node.class, () -> new Node(recoveryPolicy, task, request, parents, children));
  }

  public Node(
      RecoveryPolicy recoveryPolicy,
      SagaTask task,
      SagaRequest request,
      Map<String, List<ActorRef>> parents,
      Map<String, List<ActorRef>> children) {
    this.recoveryPolicy = recoveryPolicy;
    this.task = task;
    this.request = request;
    this.parents = parents;
    this.children = children;
    this.parentResponses = new ArrayList<>(request.parents().length);
    this.compensatedChildren = new ArrayList<>(children.get(request.id()).size());
    this.pendingParents = new HashSet<>(asList(request.parents()));

    this.transacted = onReceive(task::compensate);
    this.aborted = onReceive(ignored -> {
    });
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ResponseContext.class, this::handleContext)
        .match(Messages.class, MESSAGE_ABORT::equals, message -> getContext().become(aborted))
        .build();
  }

  private void handleContext(ResponseContext parentContext) {
    if (pendingParents.remove(parentContext.request().id())) {
      parentResponses.add(parentContext.response());
    }

    if (pendingParents.isEmpty()) {
      transact();
    }
  }

  private void transact() {
    try {
      SagaResponse sagaResponse = recoveryPolicy.apply(task, request, responseOf(parentResponses));
      children.get(request.id()).forEach(actor -> actor.tell(new ResponseContext(request, sagaResponse), self()));

      getContext().become(transacted);
    } catch (TransactionFailedException e) {
      log().error("Failed to run operation {}", request.transaction(), e);
      abort(children.values());
    }
  }

  private void abort(Collection<List<ActorRef>> allTransactions) {
    allTransactions
        .stream()
        .flatMap(Collection::stream)
        .forEach(actor -> actor.tell(MESSAGE_ABORT, self()));
  }

  private SagaResponse responseOf(List<SagaResponse> responseContexts) {
    return responseContexts.size() > 1 ? new CompositeSagaResponse(responseContexts) : responseContexts.get(0);
  }

  private Receive onReceive(Consumer<SagaRequest> requestConsumer) {
    return receiveBuilder()
        .match(Messages.class, MESSAGE_COMPENSATE::equals, message -> onCompensate(requestConsumer))
        .build();
  }

  private void onCompensate(Consumer<SagaRequest> requestConsumer) {
    compensatedChildren.add(sender());

    if (compensatedChildren.size() == children.get(request.id()).size()) {
      requestConsumer.accept(request);
      parents.get(request.id()).forEach(actor -> actor.tell(MESSAGE_COMPENSATE, self()));
    }
  }

  enum Messages {
    MESSAGE_COMPENSATE,
    MESSAGE_ABORT
  }
}
