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

import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class SagaEndTask implements SagaTask {

  private final String sagaId;
  private final SagaLog sagaLog;

  public SagaEndTask(String sagaId, SagaLog sagaLog) {
    this.sagaId = sagaId;
    this.sagaLog = sagaLog;
  }

  @Segment(name = "endTaskCommit", category = "application", library = "kamon")
  @Override
  public SagaResponse commit(SagaRequest request, SagaResponse parentResponse) {
    sagaLog.offer(new SagaEndedEvent(sagaId, request));
    return EMPTY_RESPONSE;
  }

  @Override
  public void compensate(SagaRequest request) {
  }

  @Override
  public void abort(SagaRequest request, Exception e) {
  }
}
