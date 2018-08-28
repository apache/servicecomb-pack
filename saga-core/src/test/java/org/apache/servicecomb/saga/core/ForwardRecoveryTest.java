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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class ForwardRecoveryTest {

  private final SagaTask sagaTask = mock(SagaTask.class);

  private final Transaction transaction = mock(Transaction.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaResponse parentResponse = mock(SagaResponse.class);

  private final ForwardRecovery forwardRecovery = new ForwardRecovery();

  private final String serviceName = "aaa";

  @Before
  public void setUp() {
    when(sagaRequest.serviceName()).thenReturn(serviceName);
    when(sagaRequest.transaction()).thenReturn(transaction);
    when(sagaRequest.failRetryDelayMilliseconds()).thenReturn(300);
  }

  @Test
  public void blowsUpWhenTaskIsNotCommittedWithFailRetryDelaySeconds() throws Exception {
    doThrow(Exception.class).when(transaction).send(serviceName, parentResponse);

    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        forwardRecovery.apply(sagaTask, sagaRequest, parentResponse);
      }
    });
    t.start();
    Thread.sleep(400);
    t.interrupt();

    verify(transaction, times(2)).send(serviceName, parentResponse);
  }
}
