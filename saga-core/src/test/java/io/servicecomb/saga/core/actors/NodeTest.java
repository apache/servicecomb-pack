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

import static akka.actor.ActorRef.noSender;
import static akka.actor.Props.empty;
import static io.servicecomb.saga.core.Operation.SUCCESSFUL_SAGA_RESPONSE;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static io.servicecomb.saga.core.actors.Node.Messages.MESSAGE_ABORT;
import static io.servicecomb.saga.core.actors.Node.Messages.MESSAGE_COMPENSATE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.scalatest.junit.JUnitSuite;

import com.seanyinx.github.unit.scaffolding.Randomness;

import io.servicecomb.saga.core.CompositeSagaResponse;
import io.servicecomb.saga.core.RecoveryPolicy;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.TransactionFailedException;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

@SuppressWarnings("unchecked")
public class NodeTest extends JUnitSuite {
  private final String parentRequestId1 = Randomness.uniquify("parentRequestId1");
  private final String parentRequestId2 = Randomness.uniquify("parentRequestId2");
  private final String requestId = Randomness.uniquify("requestId");
  private final String unRelatedRequestId = Randomness.uniquify("unRelatedRequestId");

  private final RecoveryPolicy recoveryPolicy = Mockito.mock(RecoveryPolicy.class);
  private final SagaTask task = Mockito.mock(SagaTask.class);
  private final SagaRequest request = Mockito.mock(SagaRequest.class, "request");
  private final SagaRequest request1 = Mockito.mock(SagaRequest.class, "request1");
  private final SagaRequest request2 = Mockito.mock(SagaRequest.class, "request2");
  private final SagaResponse response = Mockito.mock(SagaResponse.class);

  private final Map<String, List<ActorRef>> children = new HashMap<>();
  private final Map<String, List<ActorRef>> parents = new HashMap<>();

  private static final ActorSystem actorSystem = ActorSystem.create();

  @Before
  public void setUp() throws Exception {
    when(request.id()).thenReturn(requestId);

    when(request1.id()).thenReturn(parentRequestId1);
    when(request2.id()).thenReturn(parentRequestId2);

    children.put(requestId, new ArrayList<>());
    parents.put(requestId, new ArrayList<>());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void tellNodeResponseToAllChildren() throws Exception {
    new TestKit(actorSystem) {{
      children.get(requestId).add(getRef());
      children.get(requestId).add(getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});
      when(recoveryPolicy.apply(task, request, SUCCESSFUL_SAGA_RESPONSE)).thenReturn(response);

      ActorRef actorRef = actorSystem.actorOf(Node.props(recoveryPolicy, task, request, parents, children));

      actorRef.tell(new ResponseContext(request1, SUCCESSFUL_SAGA_RESPONSE), noSender());

      List<SagaResponse> responses = receiveN(2, duration("2 seconds")).stream()
          .map(o -> ((ResponseContext) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(response, response));

      verify(recoveryPolicy).apply(task, request, SUCCESSFUL_SAGA_RESPONSE);
    }};
  }

  @Test
  public void executeTransaction_OnlyWhenAllParentsResponsesAreReceived() throws Exception {
    new TestKit(actorSystem) {{
      children.get(requestId).add(getRef());
      children.get(requestId).add(getRef());

      ArgumentCaptor<SagaResponse> argumentCaptor = ArgumentCaptor.forClass(SagaResponse.class);

      when(request.parents()).thenReturn(new String[] {parentRequestId1, parentRequestId2});
      when(recoveryPolicy.apply(eq(task), eq(request), argumentCaptor.capture())).thenReturn(response);

      ActorRef actorRef = actorSystem.actorOf(Node.props(recoveryPolicy, task, request, parents, children));

      actorRef.tell(new ResponseContext(request1, SUCCESSFUL_SAGA_RESPONSE), actorSystem.actorOf(empty()));
      actorRef.tell(new ResponseContext(request1, SUCCESSFUL_SAGA_RESPONSE), actorSystem.actorOf(empty()));
      expectNoMsg(duration("500 milliseconds"));

      actorRef.tell(new ResponseContext(request2, EMPTY_RESPONSE), actorSystem.actorOf(empty()));

      List<SagaResponse> responses = receiveN(2, duration("2 seconds")).stream()
          .map(o -> ((ResponseContext) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(response, response));

      SagaResponse response = argumentCaptor.getValue();
      assertThat(response, instanceOf(CompositeSagaResponse.class));
      assertThat(((CompositeSagaResponse) response).responses(),
          containsInAnyOrder(EMPTY_RESPONSE, SUCCESSFUL_SAGA_RESPONSE));
    }};
  }

  @Test
  public void tellEveryoneElseToCompensateOnError() throws Exception {
    new TestKit(actorSystem) {{
      children.get(requestId).add(getRef());
      children.get(requestId).add(getRef());
      children.computeIfAbsent(unRelatedRequestId, k -> new ArrayList<>()).add(getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});
      when(recoveryPolicy.apply(task, request, SUCCESSFUL_SAGA_RESPONSE)).thenThrow(TransactionFailedException.class);

      ActorRef actorRef = actorSystem.actorOf(Node.props(recoveryPolicy, task, request, parents, children));

      actorRef.tell(new ResponseContext(request1, SUCCESSFUL_SAGA_RESPONSE), noSender());

      expectMsgAllOf(duration("2 seconds"), MESSAGE_ABORT, MESSAGE_ABORT, MESSAGE_ABORT);
    }};
  }

  @Test
  public void compensateIfTransactionIsCompleted() throws Exception {
    new TestKit(actorSystem) {{
      children.get(requestId).add(actorSystem.actorOf(empty()));
      children.get(requestId).add(actorSystem.actorOf(empty()));
      parents.get(requestId).add(getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(Node.props(recoveryPolicy, task, request, parents, children));

      actorRef.tell(new ResponseContext(request1, SUCCESSFUL_SAGA_RESPONSE), getRef());
      actorRef.tell(MESSAGE_ABORT, noSender());
      actorRef.tell(MESSAGE_COMPENSATE, getRef());
      actorRef.tell(MESSAGE_COMPENSATE, getRef());

      expectMsg(duration("2 seconds"), MESSAGE_COMPENSATE);
      verify(task).compensate(request);
    }};
  }

  @Test
  public void doNotCompensateIfTransactionIsNotCompleted() throws Exception {
    new TestKit(actorSystem) {{
      children.get(requestId).add(actorSystem.actorOf(empty()));
      children.get(requestId).add(actorSystem.actorOf(empty()));
      parents.get(requestId).add(getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(Node.props(recoveryPolicy, task, request, parents, children));

      actorRef.tell(MESSAGE_ABORT, noSender());
      actorRef.tell(MESSAGE_COMPENSATE, getRef());
      actorRef.tell(MESSAGE_COMPENSATE, getRef());

      expectMsg(duration("2 seconds"), MESSAGE_COMPENSATE);
      verify(task, never()).compensate(request);
    }};
  }
}