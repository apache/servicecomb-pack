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

import static akka.actor.ActorRef.noSender;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.servicecomb.saga.core.EventStore;
import org.apache.servicecomb.saga.core.NoOpSagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.application.SagaFactory;
import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaTaskFactory;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class ActorBasedSagaFactory implements SagaFactory {
  private final ActorSystem actorSystem = ActorSystem.create("saga");
  private final RequestActorBuilder actorBuilder;
  private final SagaTaskFactory sagaTaskFactory;

  public ActorBasedSagaFactory(int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat<Set<String>> childrenExtractor) {

    this.sagaTaskFactory = new SagaTaskFactory(retryDelay, persistentStore);
    this.actorBuilder = new RequestActorBuilder(actorSystem, childrenExtractor);
  }

  @Override
  public ActorBasedSaga createSaga(String requestJson, String sagaId, EventStore sagaLog, SagaDefinition definition) {

    CompletableFuture<SagaResponse> future = new CompletableFuture<>();
    ActorRef completionCallback = actorSystem.actorOf(CompletionCallbackActor.props(future));
    RequestActorContext context = actorBuilder.build(
        definition.requests(),
        sagaTaskFactory.sagaTasks(sagaId,
            requestJson,
            definition.policy(),
            sagaLog
        ),
        completionCallback);

    completionCallback.tell(context, noSender());
    return new ActorBasedSaga(
        context.actorOf(NoOpSagaRequest.SAGA_START_REQUEST.id()),
        completionCallback,
        future,
        sagaLog,
        new EventContextImpl(context));
  }

  @Override
  public boolean isTerminated() {
    return actorSystem.whenTerminated().isCompleted();
  }

  @Override
  public void terminate() throws Exception {
    Await.result(actorSystem.terminate(), Duration.Inf());
  }
}
