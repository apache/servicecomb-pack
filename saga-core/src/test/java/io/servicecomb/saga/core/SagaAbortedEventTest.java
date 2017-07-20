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

package io.servicecomb.saga.core;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.LinkedList;
import org.junit.Test;
import org.mockito.Mockito;

public class SagaAbortedEventTest {

  private final SagaState sagaState = Mockito.mock(SagaState.class);

  private final SagaAbortedEvent event = new SagaAbortedEvent(1L);
  private final LongIdGenerator idGenerator = new LongIdGenerator();
  private final LinkedList<SagaTask> emptyQueue = new LinkedList<>();

  @Test
  public void returnsCompensationStateWhenPlaying() {
    SagaState state = event.play(sagaState, emptyQueue, emptyQueue, idGenerator);

    assertThat(state, is(CompensationState.INSTANCE));
    assertThat(idGenerator.nextId(), is(2L));
  }
}