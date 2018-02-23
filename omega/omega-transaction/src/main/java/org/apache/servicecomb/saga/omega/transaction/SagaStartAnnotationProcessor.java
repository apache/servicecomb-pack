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

package org.apache.servicecomb.saga.omega.transaction;

import javax.transaction.TransactionalException;

import org.apache.servicecomb.saga.omega.context.OmegaContext;

class SagaStartAnnotationProcessor implements EventAwareInterceptor {

  private final OmegaContext omegaContext;
  private final MessageSender sender;

  SagaStartAnnotationProcessor(OmegaContext omegaContext, MessageSender sender) {
    this.omegaContext = omegaContext;
    this.sender = sender;
  }

  @Override
  public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
      int retries, Object... message) {
    try {
      return sender.send(new SagaStartedEvent(omegaContext.globalTxId(), omegaContext.localTxId(), timeout));
    } catch (OmegaException e) {
      throw new TransactionalException(e.getMessage(), e.getCause());
    }
  }

  @Override
  public void postIntercept(String parentTxId, String compensationMethod) {
    AlphaResponse response = sender.send(new SagaEndedEvent(omegaContext.globalTxId(), omegaContext.localTxId()));
    if (response.aborted()) {
      throw new OmegaException("transaction " + parentTxId + " is aborted");
    }
  }

  @Override
  public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
    String globalTxId = omegaContext.globalTxId();
    sender.send(new TxAbortedEvent(globalTxId, globalTxId, null, compensationMethod, throwable));
  }
}
