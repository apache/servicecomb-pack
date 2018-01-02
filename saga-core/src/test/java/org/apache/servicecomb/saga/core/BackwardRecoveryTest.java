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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class BackwardRecoveryTest {

  private final String serviceName = "aaa";
  private final Transaction transaction = mock(Transaction.class);
  private final SagaTask sagaTask = mock(SagaTask.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaResponse parentResponse = mock(SagaResponse.class);
  private final BackwardRecovery backwardRecovery = new BackwardRecovery();
  private final RuntimeException exception = new RuntimeException("oops");

  @Before
  public void setUp() throws Exception {
    when(sagaRequest.serviceName()).thenReturn(serviceName);
    when(sagaRequest.transaction()).thenReturn(transaction);
  }

  @Test
  public void blowsUpWhenTaskIsNotCommitted() {
    doThrow(exception).when(transaction).send(serviceName, parentResponse);

    try {
      backwardRecovery.apply(sagaTask, sagaRequest, parentResponse);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException ignored) {
    }

    verify(sagaTask).abort(sagaRequest, exception);
  }
}
