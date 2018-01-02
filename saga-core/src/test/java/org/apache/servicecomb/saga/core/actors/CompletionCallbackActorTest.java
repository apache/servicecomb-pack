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
import static akka.actor.Props.empty;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.servicecomb.saga.core.NoOpSagaRequest;
import org.apache.servicecomb.saga.core.actors.messages.AbortMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.servicecomb.saga.core.FailedSagaResponse;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.actors.messages.CompensateMessage;
import org.apache.servicecomb.saga.core.actors.messages.FailMessage;
import org.apache.servicecomb.saga.core.actors.messages.TransactMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

public class CompletionCallbackActorTest {
  private static final ActorSystem actorSystem = ActorSystem.create();

  private final SagaResponse response = Mockito.mock(SagaResponse.class);
  private final RequestActorContext context = new RequestActorContext(null);

  private final ActorRef actor1 = someActor();
  private final ActorRef actor2 = someActor();

  @Before
  public void setUp() throws Exception {
    context.addActor(uniquify("requestId"), actor1);
    context.addActor(uniquify("requestId"), actor2);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void killAllOnTransactionComplete() throws Exception {
    new TestKit(actorSystem) {{
      CompletableFuture<SagaResponse> future = new CompletableFuture<>();

      ActorRef actor = actorSystem.actorOf(CompletionCallbackActor.props(future));

      actor.tell(context, noSender());
      actor.tell(new TransactMessage(null, response), noSender());

      await().atMost(2, TimeUnit.SECONDS)
          .until(() -> actor1.isTerminated() && actor2.isTerminated() && actor.isTerminated());

      assertThat(future.get(), is(response));
    }};
  }

  @Test
  public void killAllOnCompensationComplete() throws Exception {
    new TestKit(actorSystem) {{
      CompletableFuture<SagaResponse> future = new CompletableFuture<>();

      ActorRef actor = actorSystem.actorOf(CompletionCallbackActor.props(future));

      actor.tell(context, noSender());
      actor.tell(new CompensateMessage(response), noSender());

      await().atMost(2, TimeUnit.SECONDS)
          .until(() -> actor1.isTerminated() && actor2.isTerminated() && actor.isTerminated());

      assertThat(future.get(), is(response));
    }};
  }

  @Test
  public void killAllOnFailure() throws Exception {
    new TestKit(actorSystem) {{
      CompletableFuture<SagaResponse> future = new CompletableFuture<>();

      ActorRef actor = actorSystem.actorOf(CompletionCallbackActor.props(future));

      actor.tell(context, noSender());
      actor.tell(new FailMessage(new RuntimeException("oops")), noSender());

      await().atMost(2, TimeUnit.SECONDS)
          .until(() -> actor1.isTerminated() && actor2.isTerminated() && actor.isTerminated());

      assertThat(future.get(), is(instanceOf(FailedSagaResponse.class)));
    }};
  }

  @Test
  public void tellLeafToCompensateOnAbort() throws Exception {
    new TestKit(actorSystem) {{
      context.addActor(NoOpSagaRequest.SAGA_END_REQUEST.id(), getRef());
      CompletableFuture<SagaResponse> future = new CompletableFuture<>();

      ActorRef actor = actorSystem.actorOf(CompletionCallbackActor.props(future));

      actor.tell(context, noSender());
      AbortMessage message = new AbortMessage(new RuntimeException("oops"));
      actor.tell(message, noSender());

      CompensateMessage compensateMessage = (CompensateMessage) receiveOne(duration("2 seconds"));
      assertThat(compensateMessage.response(), is(message.response()));
    }};
  }

  private ActorRef someActor() {
    return actorSystem.actorOf(empty());
  }
}
