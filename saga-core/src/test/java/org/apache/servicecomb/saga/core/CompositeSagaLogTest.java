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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
