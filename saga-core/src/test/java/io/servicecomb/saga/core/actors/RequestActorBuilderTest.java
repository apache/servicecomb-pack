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
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_END_REQUEST;
import static io.servicecomb.saga.core.NoOpSagaRequest.SAGA_START_REQUEST;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static io.servicecomb.saga.core.SagaTask.SAGA_END_TASK;
import static io.servicecomb.saga.core.SagaTask.SAGA_START_TASK;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.scalatest.junit.JUnitSuite;

import com.seanyinx.github.unit.scaffolding.Randomness;

import io.servicecomb.saga.core.CompositeSagaResponse;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SagaTask;
import io.servicecomb.saga.core.actors.messages.TransactMessage;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

@SuppressWarnings("unchecked")
@Ignore
public class RequestActorBuilderTest extends JUnitSuite {
  private static final ActorSystem actorSystem = ActorSystem.create();

  private final String requestId1 = Randomness.uniquify("requestId1");
  private final String requestId2 = Randomness.uniquify("requestId2");
  private final String requestId3 = Randomness.uniquify("requestId3");
  private final String taskId = "some task";

  private final SagaRequest request1 = Mockito.mock(SagaRequest.class);
  private final SagaRequest request2 = Mockito.mock(SagaRequest.class);
  private final SagaRequest request3 = Mockito.mock(SagaRequest.class);

  private final SagaResponse response1 = Mockito.mock(SagaResponse.class);
  private final SagaResponse response2 = Mockito.mock(SagaResponse.class);
  private final SagaResponse response3 = Mockito.mock(SagaResponse.class);

  private final SagaRequest[] requests = {request1, request2, request3};

  private final SagaTask task = Mockito.mock(SagaTask.class);
  private final Map<String, SagaTask> tasks = new HashMap<>();

  private final FromJsonFormat<Set<String>> childrenExtractor = Mockito.mock(FromJsonFormat.class);
  private final RequestActorBuilder actorBuilder = new RequestActorBuilder(actorSystem, childrenExtractor);

  @Before
  public void setUp() throws Exception {
    tasks.put(SAGA_START_TASK, task);
    tasks.put(SAGA_END_TASK, task);
    tasks.put(taskId, task);

    when(request1.id()).thenReturn(requestId1);
    when(request2.id()).thenReturn(requestId2);
    when(request3.id()).thenReturn(requestId3);

    when(request1.task()).thenReturn(taskId);
    when(request2.task()).thenReturn(taskId);
    when(request3.task()).thenReturn(taskId);

    when(request1.parents()).thenReturn(new String[0]);
    when(request2.parents()).thenReturn(new String[] {requestId1});
    when(request3.parents()).thenReturn(new String[] {requestId1});

    when(task.commit(SAGA_START_REQUEST, EMPTY_RESPONSE)).thenReturn(EMPTY_RESPONSE);
    when(task.commit(request1, EMPTY_RESPONSE)).thenReturn(response1);
    when(task.commit(request2, response1)).thenReturn(response2);
    when(task.commit(request3, response1)).thenReturn(response3);

    when(childrenExtractor.fromJson(anyString())).thenReturn(Collections.emptySet());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TestKit.shutdownActorSystem(actorSystem);
  }

  @Test
  public void createOneActorPerRequest() throws Exception {
    new TestKit(actorSystem) {{
      ArgumentCaptor<SagaResponse> argumentCaptor = ArgumentCaptor.forClass(SagaResponse.class);
      when(task.commit(eq(SAGA_END_REQUEST), argumentCaptor.capture())).thenReturn(EMPTY_RESPONSE);

      ActorRef root = actorBuilder.build(requests, tasks, getRef());

      root.tell(new TransactMessage(SAGA_START_REQUEST, EMPTY_RESPONSE), noSender());

      List<SagaResponse> responses = receiveN(1, duration("2 seconds")).stream()
          .map(o -> ((TransactMessage) o).response())
          .collect(Collectors.toList());

      assertThat(responses, containsInAnyOrder(EMPTY_RESPONSE));

      verify(task).commit(SAGA_START_REQUEST, EMPTY_RESPONSE);
      verify(task).commit(request1, EMPTY_RESPONSE);
      verify(task).commit(request2, response1);
      verify(task).commit(request3, response1);
      verify(task).commit(eq(SAGA_END_REQUEST), any(SagaResponse.class));

      SagaResponse response = argumentCaptor.getValue();
      assertThat(response, instanceOf(CompositeSagaResponse.class));
      assertThat(((CompositeSagaResponse) response).responses(),
          containsInAnyOrder(response2, response3));
    }};
  }
}