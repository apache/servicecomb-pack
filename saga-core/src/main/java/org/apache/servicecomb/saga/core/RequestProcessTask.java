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

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class RequestProcessTask implements SagaTask {

  private final String sagaId;
  private final SagaLog sagaLog;
  private final RecoveryPolicy recoveryPolicy;
  private final FallbackPolicy fallbackPolicy;

  public RequestProcessTask(
      String sagaId,
      SagaLog sagaLog,
      RecoveryPolicy recoveryPolicy,
      FallbackPolicy fallbackPolicy) {

    this.sagaId = sagaId;
    this.sagaLog = sagaLog;
    this.recoveryPolicy = recoveryPolicy;
    this.fallbackPolicy = fallbackPolicy;
  }

  @Segment(name = "commit", category = "application", library = "kamon")
  @Override
  public SagaResponse commit(SagaRequest request, SagaResponse parentResponse) {
    sagaLog.offer(new TransactionStartedEvent(sagaId, request));

    SagaResponse response = recoveryPolicy.apply(this, request, parentResponse);

    sagaLog.offer(new TransactionEndedEvent(sagaId, request, response));
    return response;
  }

  @Segment(name = "compensate", category = "application", library = "kamon")
  @Override
  public void compensate(SagaRequest request) {
    Compensation compensation = request.compensation();
    SagaResponse response = fallbackPolicy.apply(request.serviceName(), compensation, request.fallback());

    sagaLog.offer(new TransactionCompensatedEvent(sagaId, request, response));
  }

  @Override
  public void abort(SagaRequest request, Exception e) {
    sagaLog.offer(new TransactionAbortedEvent(sagaId, request, e));
  }
}
