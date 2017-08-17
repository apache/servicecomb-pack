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

package io.servicecomb.saga.core.application;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.servicecomb.saga.core.DummyEvent;
import io.servicecomb.saga.core.SagaLog;
import io.servicecomb.saga.core.SagaRequest;
import org.junit.Test;

public class CompositeSagaLogTest {

  private final SagaRequest request = mock(SagaRequest.class);
  private final DummyEvent sagaEvent = new DummyEvent(request);
  private final SagaLog embedded = mock(SagaLog.class);
  private final SagaLog persistent = mock(SagaLog.class);

  private final SagaLog compositeSagaLog = new CompositeSagaLog(embedded, persistent);

  @Test
  public void addsLogsToEmbeddedOnlyAfterPersisted() {
    doThrow(RuntimeException.class).when(persistent).offer(sagaEvent);

    try {
      compositeSagaLog.offer(sagaEvent);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException ignored) {
    }

    verify(embedded, never()).offer(sagaEvent);
  }
}