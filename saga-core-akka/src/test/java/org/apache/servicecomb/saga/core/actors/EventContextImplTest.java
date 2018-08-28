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

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.actors.messages.AbortRecoveryMessage;
import org.apache.servicecomb.saga.core.actors.messages.CompensationRecoveryMessage;
import org.apache.servicecomb.saga.core.actors.messages.TransactionRecoveryMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.scalatest.junit.JUnitSuite;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

public class EventContextImplTest extends JUnitSuite {
  private static final ActorSystem actorSystem = ActorSystem.create();

  private final SagaRequest request = Mockito.mock(SagaRequest.class);
  private final SagaResponse response = Mockito.mock(SagaResponse.class);

  private final RequestActorContext context = new RequestActorContext(null);
  private final EventContextImpl eventContext = new EventContextImpl(context);
  private final String requestId = uniquify("requestId");

  @Before
  public void setUp() throws Exception {
    when(request.id()).thenReturn(requestId);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void sendTransactionRecoveryMessageToActor_OnTransactionEnd() throws Exception {
    new TestKit(actorSystem) {{
      context.addActor(requestId, getRef());

      eventContext.endTransaction(request, response);

      TransactionRecoveryMessage message = (TransactionRecoveryMessage) receiveOne(Duration.of(2, SECONDS));
      assertThat(message.response(), is(response));
    }};
  }

  @Test
  public void sendCompensationRecoveryMessageToActor_OnCompensationEnd() throws Exception {
    new TestKit(actorSystem) {{
      context.addActor(requestId, getRef());

      eventContext.compensateTransaction(request, response);

      expectMsgClass(CompensationRecoveryMessage.class);
    }};
  }

  @Test
  public void sendAbortMessageToActor_OnAbort() throws Exception {
    new TestKit(actorSystem) {{
      context.addActor(requestId, getRef());

      eventContext.abortTransaction(request, response);

      AbortRecoveryMessage message = ((AbortRecoveryMessage) receiveOne(Duration.of(2, SECONDS)));
      assertThat(message.response(), is(response));
    }};
  }
}
