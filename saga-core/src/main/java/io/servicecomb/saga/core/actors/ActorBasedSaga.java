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

import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;

import java.util.concurrent.CompletableFuture;

import io.servicecomb.saga.core.EventContext;
import io.servicecomb.saga.core.EventStore;
import io.servicecomb.saga.core.Saga;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.actors.messages.TransactMessage;
import akka.actor.ActorRef;

public class ActorBasedSaga implements Saga {
  private final ActorRef root;
  private final ActorRef completionCallback;
  private final CompletableFuture<SagaResponse> future;
  private final EventStore sagaLog;
  private final EventContext sagaContext;

  ActorBasedSaga(ActorRef root, ActorRef completionCallback, CompletableFuture<SagaResponse> future, EventStore sagaLog,
      EventContext sagaContext) {
    this.root = root;
    this.completionCallback = completionCallback;
    this.future = future;
    this.sagaLog = sagaLog;
    this.sagaContext = sagaContext;
  }

  @Override
  public SagaResponse run() {
    root.tell(new TransactMessage(SAGA_START_REQUEST, EMPTY_RESPONSE), completionCallback);

    return future.join();
  }

  @Override
  public void play() {
    gatherEvents(sagaLog);
  }

  private void gatherEvents(Iterable<SagaEvent> events) {
    for (SagaEvent event : events) {
      event.gatherTo(sagaContext);
    }
  }
}
