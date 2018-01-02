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

package org.apache.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SagaStartTaskTest {
  private final SagaRequest request = mock(SagaRequest.class);
  private final SagaLog sagaLog = mock(SagaLog.class);

  private final String sagaId = "0";
  private final String requestJson = null;
  private final SagaStartTask sagaStartTask = new SagaStartTask(sagaId, requestJson, sagaLog);

  @Test
  public void emptyResponseOnSuccessfulEventPersistence() throws Exception {
    ArgumentCaptor<SagaStartedEvent> argumentCaptor = ArgumentCaptor.forClass(SagaStartedEvent.class);
    doNothing().when(sagaLog).offer(argumentCaptor.capture());

    sagaStartTask.commit(request, SagaResponse.EMPTY_RESPONSE);

    SagaStartedEvent event = argumentCaptor.getValue();
    assertThat(event.sagaId, is(sagaId));
    assertThat(event.json(null), is(requestJson));
    assertThat(event.payload(), is(request));
  }

  @Test
  public void blowsUpWhenEventIsNotPersisted() {
    doThrow(RuntimeException.class).when(sagaLog).offer(any(SagaStartedEvent.class));

    try {
      sagaStartTask.commit(request, SagaResponse.EMPTY_RESPONSE);
      expectFailing(SagaStartFailedException.class);
    } catch (SagaStartFailedException e) {
      assertThat(e.getMessage(), startsWith("Failed to persist SagaStartedEvent"));
    }
  }
}
