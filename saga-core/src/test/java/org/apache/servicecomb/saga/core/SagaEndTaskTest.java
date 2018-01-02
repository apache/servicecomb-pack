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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SagaEndTaskTest {
  private final SagaRequest request = mock(SagaRequest.class);
  private final SagaLog sagaLog = mock(SagaLog.class);

  private final String sagaId = "0";
  private final SagaEndTask sagaEndTask = new SagaEndTask(sagaId, sagaLog);

  @Test
  public void emptyResponseOnSuccessfulEventPersistence() throws Exception {
    ArgumentCaptor<SagaEndedEvent> argumentCaptor = ArgumentCaptor.forClass(SagaEndedEvent.class);
    doNothing().when(sagaLog).offer(argumentCaptor.capture());

    sagaEndTask.commit(request, SagaResponse.EMPTY_RESPONSE);

    SagaEndedEvent event = argumentCaptor.getValue();
    assertThat(event.sagaId, is(sagaId));
    assertThat(event.json(null), is("{}"));
    assertThat(event.payload(), is(request));
  }
}
