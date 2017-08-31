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

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class RequestProcessTask implements SagaTask {

  private final String sagaId;
  private final SagaLog sagaLog;
  private final Transport transport;

  public RequestProcessTask(String sagaId, SagaLog sagaLog, Transport transport) {
    this.sagaId = sagaId;
    this.sagaLog = sagaLog;
    this.transport = transport;
  }

  @Segment(name = "commit", category = "application", library = "kamon")
  @Override
  public void commit(SagaRequest request) {
    sagaLog.offer(new TransactionStartedEvent(sagaId, request));

    Transaction transaction = request.transaction();
    SagaResponse response = transport.with(
        request.serviceName(),
        transaction.path(),
        transaction.method(),
        transaction.params());

    sagaLog.offer(new TransactionEndedEvent(sagaId, request, response));
  }

  @Segment(name = "compensate", category = "application", library = "kamon")
  @Override
  public void compensate(SagaRequest request) {
    sagaLog.offer(new CompensationStartedEvent(sagaId, request));

    Compensation compensation = request.compensation();
    SagaResponse response = transport
        .with(request.serviceName(), compensation.path(), compensation.method(), compensation.params());

    sagaLog.offer(new CompensationEndedEvent(sagaId, request, response));
  }

  @Override
  public void abort(SagaRequest request, Exception e) {
    sagaLog.offer(new TransactionAbortedEvent(sagaId, request, e));
  }
}
