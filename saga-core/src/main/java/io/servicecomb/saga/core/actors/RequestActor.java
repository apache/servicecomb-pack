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

import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static io.servicecomb.saga.core.SagaResponse.NONE_RESPONSE;
import static io.servicecomb.saga.core.actors.messages.AbortMessage.MESSAGE_ABORT;
import static io.servicecomb.saga.core.actors.messages.CompensateMessage.MESSAGE_COMPENSATE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import io.servicecomb.saga.core.CompositeSagaResponse;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.TransactionFailedException;
import io.servicecomb.saga.core.actors.messages.AbortMessage;
import io.servicecomb.saga.core.actors.messages.CompensateMessage;
import io.servicecomb.saga.core.actors.messages.TransactMessage;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class RequestActor extends AbstractLoggingActor {
  private final RequestActorContext context;
  private final SagaTask task;
  private final SagaRequest request;

  private final List<SagaResponse> parentResponses;
  private final List<ActorRef> compensatedChildren;

  private final Receive transacted;
  private final Receive aborted;

  static Props props(
      RequestActorContext context,
      SagaTask task,
      SagaRequest request) {
    return Props.create(RequestActor.class, () -> new RequestActor(context, task, request));
  }

  private RequestActor(
      RequestActorContext context,
      SagaTask task,
      SagaRequest request) {
    this.context = context;
    this.task = task;
    this.request = request;
    this.parentResponses = new ArrayList<>(request.parents().length);
    this.compensatedChildren = new LinkedList<>();

    this.transacted = onReceive(task::compensate);
    this.aborted = onReceive(ignored -> {
    });
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(TransactMessage.class, this::handleContext)
        .match(AbortMessage.class, message -> getContext().become(aborted))
        .build();
  }

  private void handleContext(TransactMessage parentContext) {
    if (context.parentsOf(request).contains(sender())) {
      parentResponses.add(parentContext.response());
    }

    if (parentResponses.size() == context.parentsOf(request).size()) {
      transact();
    }
  }

  private void transact() {
    try {
      if (isChosenChild(parentResponses)) {
        SagaResponse sagaResponse = task.commit(request, responseOf(parentResponses));
        context.childrenOf(request).forEach(actor -> actor.tell(new TransactMessage(request, sagaResponse), self()));
        getContext().become(transacted);
      } else {
        context.childrenOf(request).forEach(actor -> actor.tell(new TransactMessage(request, NONE_RESPONSE), self()));
        getContext().become(aborted);
      }
    } catch (TransactionFailedException e) {
      log().error("Failed to run operation {} with error {}", request.transaction(), e);
      context.forAll(actor -> actor.tell(MESSAGE_ABORT, self()));
    }
  }

  private boolean isChosenChild(List<SagaResponse> parentResponses) {
    return parentResponses.isEmpty() || parentResponses.stream()
            .map(context::chosenChildren)
            .anyMatch(chosenChildren -> chosenChildren.isEmpty() || chosenChildren.contains(request.id()));
  }

  private SagaResponse responseOf(List<SagaResponse> responseContexts) {
    if (responseContexts.isEmpty()) {
      return EMPTY_RESPONSE;
    }

    if (responseContexts.size() == 1) {
      return responseContexts.get(0);
    }
    return new CompositeSagaResponse(responseContexts);
  }

  private Receive onReceive(Consumer<SagaRequest> requestConsumer) {
    return receiveBuilder()
        .match(CompensateMessage.class, message -> onCompensate(requestConsumer))
        .build();
  }

  private void onCompensate(Consumer<SagaRequest> requestConsumer) {
    compensatedChildren.add(sender());

    if (compensatedChildren.size() == context.childrenOf(request).size()) {
      requestConsumer.accept(request);
      context.parentsOf(request).forEach(actor -> actor.tell(MESSAGE_COMPENSATE, self()));
    }
  }
}
