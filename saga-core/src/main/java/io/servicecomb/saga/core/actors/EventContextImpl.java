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

package io.servicecomb.saga.core.actors;

import static akka.actor.ActorRef.noSender;

import io.servicecomb.saga.core.EventContext;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.actors.messages.AbortRecoveryMessage;
import io.servicecomb.saga.core.actors.messages.CompensationRecoveryMessage;
import io.servicecomb.saga.core.actors.messages.Message;
import io.servicecomb.saga.core.actors.messages.TransactionRecoveryMessage;

public class EventContextImpl implements EventContext {
  private final RequestActorContext context;

  EventContextImpl(RequestActorContext context) {
    this.context = context;
  }

  @Override
  public void beginTransaction(SagaRequest request) {

  }

  @Override
  public void endTransaction(SagaRequest request, SagaResponse response) {
    sendMessage(request, new TransactionRecoveryMessage(response));
  }

  @Override
  public void abortTransaction(SagaRequest request, SagaResponse response) {
    sendMessage(request, new AbortRecoveryMessage(response));
  }

  @Override
  public void compensateTransaction(SagaRequest request, SagaResponse response) {
    sendMessage(request, new CompensationRecoveryMessage());
  }

  private void sendMessage(SagaRequest request, Message message) {
    context.actorOf(request.id()).tell(message, noSender());
  }
}
