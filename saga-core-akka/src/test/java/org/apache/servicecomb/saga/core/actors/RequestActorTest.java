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
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.servicecomb.saga.core.CompositeSagaResponse;
import org.apache.servicecomb.saga.core.FailedSagaResponse;
import org.apache.servicecomb.saga.core.Operation;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SagaStartFailedException;
import org.apache.servicecomb.saga.core.SagaTask;
import org.apache.servicecomb.saga.core.TransactionFailedException;
import org.apache.servicecomb.saga.core.actors.messages.AbortMessage;
import org.apache.servicecomb.saga.core.actors.messages.CompensationRecoveryMessage;
import org.apache.servicecomb.saga.core.actors.messages.TransactMessage;
import org.apache.servicecomb.saga.core.actors.messages.TransactionRecoveryMessage;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.scalatest.junit.JUnitSuite;

import org.apache.servicecomb.saga.core.actors.messages.CompensateMessage;
import org.apache.servicecomb.saga.core.actors.messages.FailMessage;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

@SuppressWarnings("unchecked")
public class RequestActorTest extends JUnitSuite {

  public static final Duration TWO_SECONDS = Duration.of(2, SECONDS);

  private final String parentRequestId1 = uniquify("parentRequestId1");
  private final String parentRequestId2 = uniquify("parentRequestId2");
  private final String requestId = uniquify("requestId");

  private final SagaTask task = Mockito.mock(SagaTask.class);
  private final SagaRequest request = Mockito.mock(SagaRequest.class, "request");
  private final SagaRequest request1 = Mockito.mock(SagaRequest.class, "request1");
  private final SagaRequest request2 = Mockito.mock(SagaRequest.class, "request2");
  private final SagaResponse response = Mockito.mock(SagaResponse.class);
  private final FromJsonFormat<Set<String>> childrenExtractor = mock(FromJsonFormat.class);

  private final RequestActorContext context = new RequestActorContext(childrenExtractor);

  private final TransactionFailedException exception = new TransactionFailedException("oops");
  private static final ActorSystem actorSystem = ActorSystem.create();
  private final CompensateMessage compensateMessage = new CompensateMessage(new FailedSagaResponse(exception));

  @Before
  public void setUp() throws Exception {
    when(childrenExtractor.fromJson(anyString())).thenReturn(emptySet());
    when(request.id()).thenReturn(requestId);

    when(request1.id()).thenReturn(parentRequestId1);
    when(request2.id()).thenReturn(parentRequestId2);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void tellNodeResponseToAllChildren() throws Exception {
    new TestKit(actorSystem) {{
      addChildren(getRef());

      ActorRef parent = someActor();
      context.addParent(requestId, parent);

      when(request.parents()).thenReturn(new String[] {parentRequestId1});
      when(task.commit(request, Operation.SUCCESSFUL_SAGA_RESPONSE)).thenReturn(response);

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), parent);

      List<SagaResponse> responses = receiveN(2, TWO_SECONDS).stream()
          .map(o -> ((TransactMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(response, response));

      verify(task).commit(request, Operation.SUCCESSFUL_SAGA_RESPONSE);
    }};
  }

  @Test
  public void executeTransaction_OnlyWhenAllParentsResponsesAreReceived() throws Exception {
    new TestKit(actorSystem) {{
      addChildren(getRef());

      ActorRef parent1 = someActor();
      context.addParent(requestId, parent1);

      ActorRef parent2 = someActor();
      context.addParent(requestId, parent2);

      ArgumentCaptor<SagaResponse> argumentCaptor = ArgumentCaptor.forClass(SagaResponse.class);

      when(request.parents()).thenReturn(new String[] {parentRequestId1, parentRequestId2});
      when(task.commit(eq(request), argumentCaptor.capture())).thenReturn(response);

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), parent1);
      expectNoMessage(Duration.of(500, MILLIS));

      actorRef.tell(new TransactMessage(request2, SagaResponse.EMPTY_RESPONSE), parent2);

      List<SagaResponse> responses = receiveN(2, TWO_SECONDS).stream()
          .map(o -> ((TransactMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(response, response));

      SagaResponse response = argumentCaptor.getValue();
      assertThat(response, instanceOf(CompositeSagaResponse.class));
      assertThat(((CompositeSagaResponse) response).responses(),
          Matchers.containsInAnyOrder(SagaResponse.EMPTY_RESPONSE, Operation.SUCCESSFUL_SAGA_RESPONSE));
    }};
  }

  @Test
  public void tellAllRelativesToAbortOnError() throws Exception {
    new TestKit(actorSystem) {{
      context.addChild(requestId, getRef());
      context.addActor(requestId, getRef());

      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});
      when(task.commit(request, Operation.SUCCESSFUL_SAGA_RESPONSE)).thenThrow(exception);

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), getRef());

      List<SagaResponse> responses = receiveN(2, TWO_SECONDS).stream()
          .map(o -> ((AbortMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(instanceOf(FailedSagaResponse.class), instanceOf(FailedSagaResponse.class)));
    }};
  }

  @Test
  public void tellAllRelativesExceptSenderToAbortOnAbort() throws Exception {
    new TestKit(actorSystem) {{
      context.addChild(requestId, getRef());
      context.addActor(requestId, getRef());

      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new AbortMessage(exception), someActor());

      List<SagaResponse> responses = receiveN(2, TWO_SECONDS).stream()
          .map(o -> ((AbortMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(instanceOf(FailedSagaResponse.class), instanceOf(FailedSagaResponse.class)));

      actorRef.tell(new AbortMessage(exception), someActor());
      expectNoMessage(Duration.of(500, MILLIS));
    }};
  }

  @Test
  public void compensateIfTransactionIsCompleted() throws Exception {
    new TestKit(actorSystem) {{
      addChildren(someActor());
      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});
      when(task.commit(request, Operation.SUCCESSFUL_SAGA_RESPONSE)).thenReturn(response);

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), getRef());
      actorRef.tell(new AbortMessage(exception), noSender());
      actorRef.tell(compensateMessage, getRef());
      actorRef.tell(compensateMessage, getRef());

      expectMsg(TWO_SECONDS, compensateMessage);
      verify(task).compensate(request);

      // no duplicate compensation
      reset(task);
      actorRef.tell(compensateMessage, getRef());
      actorRef.tell(compensateMessage, getRef());
      expectNoMessage(Duration.of(200, MILLIS));
      verify(task, never()).compensate(request);
    }};
  }

  @Test
  public void doNotCompensateIfTransactionIsNotCompleted() throws Exception {
    new TestKit(actorSystem) {{
      addChildren(someActor());
      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new AbortMessage(exception), noSender());
      actorRef.tell(compensateMessage, getRef());
      actorRef.tell(compensateMessage, getRef());

      List<Object> responses = receiveN(2, TWO_SECONDS);
      assertThat(responses, contains(instanceOf(AbortMessage.class), instanceOf(CompensateMessage.class)));
      verify(task, never()).compensate(request);

      // no duplicate compensation
      reset(task);
      actorRef.tell(compensateMessage, getRef());
      actorRef.tell(compensateMessage, getRef());
      expectNoMessage(Duration.of(200, MILLIS));
      verify(task, never()).compensate(request);
    }};
  }

  @Test
  public void skipIfActorIsNotChosenByAnyParent() throws Exception {
    when(childrenExtractor.fromJson(Operation.SUCCESSFUL_SAGA_RESPONSE.body())).thenReturn(singleton("none"));

    new TestKit(actorSystem) {{
      addChildren(getRef());
      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), getRef());

      List<SagaResponse> responses = receiveN(2, TWO_SECONDS).stream()
          .map(o -> ((TransactMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, Matchers.containsInAnyOrder(SagaResponse.NONE_RESPONSE, SagaResponse.NONE_RESPONSE));
      verify(task, never()).commit(request, Operation.SUCCESSFUL_SAGA_RESPONSE);

      // skip compensation for ignored actor
      actorRef.tell(compensateMessage, getRef());
      actorRef.tell(compensateMessage, getRef());

      expectMsg(TWO_SECONDS, compensateMessage);
      verify(task, never()).compensate(request);
    }};
  }

  @Test
  public void transactIfChosenByAnyParent() throws Exception {
    when(childrenExtractor.fromJson(Operation.SUCCESSFUL_SAGA_RESPONSE.body())).thenReturn(singleton(requestId));

    new TestKit(actorSystem) {{
      addChildren(getRef());

      ActorRef parent1 = someActor();
      context.addParent(requestId, parent1);

      ActorRef parent2 = someActor();
      context.addParent(requestId, parent2);

      when(request.parents()).thenReturn(new String[] {parentRequestId1, parentRequestId2});
      when(task.commit(eq(request), any(CompositeSagaResponse.class))).thenReturn(response);

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), parent1);
      actorRef.tell(new TransactMessage(request2, Operation.SUCCESSFUL_SAGA_RESPONSE), parent2);

      List<SagaResponse> responses = receiveN(2, TWO_SECONDS).stream()
          .map(o -> ((TransactMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(response, response));
    }};
  }

  @Test
  public void tellTransactionResponseToChildrenOnRecovery() throws Exception {
    new TestKit(actorSystem) {{
      context.addChild(requestId, getRef());

      ActorRef parent = someActor();
      context.addParent(requestId, parent);

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactionRecoveryMessage(response), noSender());
      actorRef.tell(new TransactMessage(request1, Operation.SUCCESSFUL_SAGA_RESPONSE), parent);

      List<SagaResponse> responses = receiveN(1, TWO_SECONDS).stream()
          .map(o -> ((TransactMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(response));

      verify(task, never()).commit(request, Operation.SUCCESSFUL_SAGA_RESPONSE);
    }};
  }

  @Test
  public void tellCompensationToParentsOnRecovery() throws Exception {
    new TestKit(actorSystem) {{
      context.addChild(requestId, someActor());
      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new AbortMessage(exception), someActor());
      actorRef.tell(new CompensationRecoveryMessage(), someActor());
      actorRef.tell(compensateMessage, someActor());

      List<Object> responses = receiveN(2, TWO_SECONDS);
      assertThat(responses, contains(instanceOf(AbortMessage.class), instanceOf(CompensateMessage.class)));

      verify(task, never()).compensate(request);
    }};
  }

  @Test
  public void abortOnPersistenceFailure() throws Exception {
    new TestKit(actorSystem) {{
      context.addChild(requestId, noSender());
      context.addParent(requestId, getRef());

      when(request.parents()).thenReturn(new String[] {parentRequestId1});

      SagaStartFailedException oops = new SagaStartFailedException("oops", exception);
      when(task.commit(request, SagaResponse.EMPTY_RESPONSE)).thenThrow(oops);

      ActorRef actorRef = actorSystem.actorOf(RequestActor.props(context, task, request));

      actorRef.tell(new TransactMessage(request, SagaResponse.EMPTY_RESPONSE), getRef());

      expectMsgClass(FailMessage.class);
    }};
  }

  private ActorRef someActor() {
    return actorSystem.actorOf(empty());
  }

  private void addChildren(ActorRef ref) {
    context.addChild(requestId, ref);
    context.addChild(requestId, ref);
    context.addActor(requestId, ref);
  }
}
