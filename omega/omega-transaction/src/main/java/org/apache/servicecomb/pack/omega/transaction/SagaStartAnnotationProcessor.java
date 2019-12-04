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

package org.apache.servicecomb.pack.omega.transaction;

import javax.transaction.TransactionalException;
import org.apache.servicecomb.pack.omega.context.OmegaContext;

public class SagaStartAnnotationProcessor {

  private final OmegaContext omegaContext;
  private final SagaMessageSender sender;

  public SagaStartAnnotationProcessor(OmegaContext omegaContext, SagaMessageSender sender) {
    this.omegaContext = omegaContext;
    this.sender = sender;
  }

  public AlphaResponse preIntercept(int timeout) {
    try {
      return sender
          .send(new SagaStartedEvent(omegaContext.globalTxId(), omegaContext.localTxId(), timeout));
    } catch (OmegaException e) {
      throw new TransactionalException(e.getMessage(), e.getCause());
    }
  }

  public void postIntercept(String parentTxId) {
    AlphaResponse response = sender
        .send(new SagaEndedEvent(omegaContext.globalTxId(), omegaContext.localTxId()));
    //TODO we may know if the transaction is aborted from fsm alpha backend
    if (response.aborted()) {
      throw new OmegaException("transaction " + parentTxId + " is aborted");
    }
  }

  public void onError(String compensationMethod, Throwable throwable) {
    String globalTxId = omegaContext.globalTxId();
    if(omegaContext.getAlphaMetas().isAkkaEnabled()){
      sender.send(
          new SagaAbortedEvent(globalTxId, omegaContext.localTxId(), null, compensationMethod,
              throwable));
    }else{
      sender.send(
          new TxAbortedEvent(globalTxId, omegaContext.localTxId(), null, compensationMethod,
              throwable));
    }
  }
}
