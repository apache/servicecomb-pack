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

import java.util.Map;
import java.util.Set;

public abstract class SagaEvent implements Descriptive {

  public final String sagaId;
  private final SagaRequest payload;

  public SagaEvent(String sagaId, SagaRequest payload) {
    this.sagaId = sagaId;
    this.payload = payload;
  }

  public SagaRequest payload() {
    return payload;
  }

  public abstract void gatherTo(
      Map<String, SagaRequest> hangingTransactions,
      Set<String> abortedTransactions,
      Set<String> completedTransactions,
      Set<String> completedCompensations);

  public String json(ToJsonFormat toJsonFormat) {
    return "{}";
  }
}
