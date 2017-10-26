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
import static io.servicecomb.saga.core.actors.messages.CompensateMessage.MESSAGE_COMPENSATE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.servicecomb.saga.core.CompositeSagaResponse;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SagaStartFailedException;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.TransactionFailedException;
import io.servicecomb.saga.core.actors.messages.AbortMessage;
import io.servicecomb.saga.core.actors.messages.CompensateMessage;
import io.servicecomb.saga.core.actors.messages.CompensationRecoveryMessage;
import io.servicecomb.saga.core.actors.messages.TransactionRecoveryMessage;
import io.servicecomb.saga.core.actors.messages.TransactMessage;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

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

    this.aborted = onReceive(ignored -> {
    }).build();

    this.transacted = onReceive(task::compensate)
        .match(CompensationRecoveryMessage.class, message -> getContext().become(aborted))
        .build();
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(TransactMessage.class,
            message -> handleTransaction(message, () -> task.commit(request, responseOf(parentResponses))))
        .match(TransactionRecoveryMessage.class, this::handleRecovery)
        .match(AbortMessage.class, message -> getContext().become(aborted))
        .build();
  }

  private void handleRecovery(TransactionRecoveryMessage message) {
    getContext().become(receiveBuilder()
        .match(TransactMessage.class, m -> handleTransaction(m, message::response))
        .build()
    );
  }

  private void handleTransaction(TransactMessage message, Supplier<SagaResponse> responseSupplier) {
    if (context.parentsOf(request).contains(sender())) {
      parentResponses.add(message.response());
    }

    if (parentResponses.size() == context.parentsOf(request).size()) {
      transact(responseSupplier);
    }
  }

  private void transact(Supplier<SagaResponse> responseSupplier) {
    try {
      if (isChosenChild(parentResponses)) {
        SagaResponse sagaResponse = responseSupplier.get();
        context.childrenOf(request).forEach(actor -> actor.tell(new TransactMessage(request, sagaResponse), self()));
        getContext().become(transacted);
      } else {
        context.childrenOf(request).forEach(actor -> actor.tell(new TransactMessage(request, NONE_RESPONSE), self()));
        getContext().become(aborted);
      }
    } catch (TransactionFailedException e) {
      log().error("Failed to run operation {} with error {}", request.transaction(), e);
      context.forAll(actor -> actor.tell(new AbortMessage(e), self()));
    } catch (SagaStartFailedException e) {
      context.parentsOf(request).forEach(actor -> actor.tell(new AbortMessage(e), self()));
      getContext().stop(self());
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

  private ReceiveBuilder onReceive(Consumer<SagaRequest> requestConsumer) {
    return receiveBuilder()
        .match(CompensateMessage.class, message -> onCompensate(requestConsumer));
  }

  private void onCompensate(Consumer<SagaRequest> requestConsumer) {
    compensatedChildren.add(sender());

    if (compensatedChildren.size() == context.childrenOf(request).size()) {
      requestConsumer.accept(request);
      context.parentsOf(request).forEach(actor -> actor.tell(MESSAGE_COMPENSATE, self()));
      getContext().stop(self());
    }
  }
}
