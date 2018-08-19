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

package org.apache.servicecomb.saga.core.actors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.servicecomb.saga.core.CompositeSagaResponse;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SagaStartFailedException;
import org.apache.servicecomb.saga.core.SagaTask;
import org.apache.servicecomb.saga.core.TransactionFailedException;
import org.apache.servicecomb.saga.core.actors.messages.AbortMessage;
import org.apache.servicecomb.saga.core.actors.messages.AbortRecoveryMessage;
import org.apache.servicecomb.saga.core.actors.messages.CompensationRecoveryMessage;
import org.apache.servicecomb.saga.core.actors.messages.FailMessage;
import org.apache.servicecomb.saga.core.actors.messages.Message;
import org.apache.servicecomb.saga.core.actors.messages.TransactMessage;
import org.apache.servicecomb.saga.core.actors.messages.TransactionRecoveryMessage;
import org.apache.servicecomb.saga.core.actors.messages.CompensateMessage;

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
            message -> onTransaction(message, () -> task.commit(request, responseOf(parentResponses))))
        .match(TransactionRecoveryMessage.class, this::onTransactRecovery)
        .match(AbortRecoveryMessage.class, this::onAbortRecovery)
        .match(AbortMessage.class, this::onAbort)
        .build();
  }

  private void onAbort(AbortMessage message) {
    log().debug("{}: received abort message of {}", request.id(), message.response());
    sendToChildrenButSender(message);
    sendToParentsButSender(message);

    getContext().become(aborted);
  }

  private void sendToParentsButSender(AbortMessage message) {
    context.parentsOf(request)
        .stream()
        .filter(this::isNotSender)
        .forEach(actor -> actor.tell(message, self()));
  }

  private void sendToChildrenButSender(AbortMessage message) {
    context.childrenOf(request)
        .stream()
        .filter(this::isNotSender)
        .forEach(actor -> actor.tell(message, self()));
  }

  private boolean isNotSender(ActorRef actor) {
    return !actor.equals(sender());
  }

  private void onTransactRecovery(TransactionRecoveryMessage message) {
    getContext().become(receiveBuilder()
        .match(TransactMessage.class, m -> onTransaction(m, message::response))
        .match(CompensationRecoveryMessage.class, m -> getContext().become(aborted))
        .build()
    );
  }

  private void onAbortRecovery(AbortRecoveryMessage message) {
    getContext().become(
        receiveBuilder()
            .match(TransactMessage.class, m -> onAbort(new AbortMessage(message.response())))
            .build());
  }

  private void onTransaction(TransactMessage message, Supplier<SagaResponse> responseSupplier) {
    log().debug("{}: received transaction message of {}", request.id(), message.request());
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
        sendToChildren(new TransactMessage(request, sagaResponse));
        getContext().become(transacted);
      } else {
        sendToChildren(new TransactMessage(request, SagaResponse.NONE_RESPONSE));
        getContext().become(aborted);
      }
    } catch (SagaStartFailedException e) {
      sendToParents(new FailMessage(e));
    } catch (Exception e) {
      log().error("Failed to run operation {} with error {}", request.transaction(), e);

      Message abortMessage = new AbortMessage(new TransactionFailedException(e));
      sendToParents(abortMessage);
      sendToChildren(abortMessage);
      getContext().become(aborted);
    }
  }

  private void sendToParents(Message message) {
    context.parentsOf(request).forEach(actor -> actor.tell(message, self()));
  }

  private void sendToChildren(Message message) {
    context.childrenOf(request).forEach(actor -> actor.tell(message, self()));
  }

  private boolean isChosenChild(List<SagaResponse> parentResponses) {
    return request.parents().length == 0 || parentResponses.isEmpty() || parentResponses.stream()
            .map(context::chosenChildren)
            .anyMatch(chosenChildren -> chosenChildren.isEmpty() || chosenChildren.contains(request.id()));
  }

  private SagaResponse responseOf(List<SagaResponse> responseContexts) {
    if (responseContexts.isEmpty()) {
      return SagaResponse.EMPTY_RESPONSE;
    }

    if (responseContexts.size() == 1) {
      return responseContexts.get(0);
    }
    return new CompositeSagaResponse(responseContexts);
  }

  private ReceiveBuilder onReceive(Consumer<SagaRequest> requestConsumer) {
    return receiveBuilder()
        .match(CompensateMessage.class, message -> onCompensate(message, requestConsumer));
  }

  private void onCompensate(CompensateMessage message, Consumer<SagaRequest> requestConsumer) {
    log().debug("{}: received compensation message from {}", request.id(), sender());
    compensatedChildren.add(sender());

    if (compensatedChildren.size() == context.childrenOf(request).size()) {
      requestConsumer.accept(request);
      sendToParents(message);
    }
  }
}
