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
import static io.servicecomb.saga.core.actors.messages.CompensateMessage.MESSAGE_COMPENSATE;

import java.util.concurrent.CompletableFuture;

import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.actors.messages.AbortMessage;
import io.servicecomb.saga.core.actors.messages.CompensateMessage;
import io.servicecomb.saga.core.actors.messages.FailMessage;
import io.servicecomb.saga.core.actors.messages.TransactMessage;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
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
        .match(ActorRef.class, this::ready)
        .build();
  }

  private void ready(ActorRef leaf) {
    getContext().become(receiveBuilder()
        .match(CompensateMessage.class, message -> {
          future.complete(EMPTY_RESPONSE);
          getContext().stop(self());
        })
        .match(TransactMessage.class, message -> future.complete(message.response()))
        .match(AbortMessage.class, message -> leaf.tell(MESSAGE_COMPENSATE, self()))
        .match(FailMessage.class, message -> future.complete(message.response()))
        .build());
  }
}
