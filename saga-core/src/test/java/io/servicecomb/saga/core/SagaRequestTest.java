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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class SagaRequestTest {

  private final Transaction transaction = mock(Transaction.class);
  private final Compensation compensation = mock(Compensation.class);

  private final SagaRequest request = new SagaRequest(transaction, compensation);

  @Test
  public void runTransactionOnCommit() {
    request.commit();

    verify(transaction).run();
  }

  @Test
  public void runCompensationOnAbort() {
    request.abort();

    verify(compensation).run();
  }
}