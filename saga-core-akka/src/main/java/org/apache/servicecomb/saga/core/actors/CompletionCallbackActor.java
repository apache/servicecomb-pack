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

import java.util.concurrent.CompletableFuture;

import org.apache.servicecomb.saga.core.NoOpSagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.actors.messages.AbortMessage;
import org.apache.servicecomb.saga.core.actors.messages.CompensateMessage;
import org.apache.servicecomb.saga.core.actors.messages.FailMessage;
import org.apache.servicecomb.saga.core.actors.messages.TransactMessage;
import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

class CompletionCallbackActor extends AbstractLoggingActor {
  private final CompletableFuture<SagaResponse> future;

  private CompletionCallbackActor(CompletableFuture<SagaResponse> future) {
    this.future = future;
  }

  static Props props(CompletableFuture<SagaResponse> future) {
    return Props.create(CompletionCallbackActor.class, () -> new CompletionCallbackActor(future));
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(RequestActorContext.class, this::ready)
        .build();
  }

  private void ready(RequestActorContext context) {
    getContext().become(receiveBuilder()
        .match(CompensateMessage.class, message -> end(context, message.response()))
        .match(TransactMessage.class, message -> end(context, message.response()))
        .match(AbortMessage.class, message -> onAbort(context, message))
        .match(FailMessage.class, message -> end(context, message.response()))
        .build());
  }

  private void onAbort(RequestActorContext context, AbortMessage message) {
    log().info("saga actor: received abort message of {}", message.response());
    context.actorOf(NoOpSagaRequest.SAGA_END_REQUEST.id()).tell(new CompensateMessage(message.response()), self());
  }

  private void end(RequestActorContext context, SagaResponse response) {
    log().info("saga actor: received response {}", response);
    future.complete(response);
    context.forAll(actor -> getContext().stop(actor));
    getContext().stop(self());
  }
}
