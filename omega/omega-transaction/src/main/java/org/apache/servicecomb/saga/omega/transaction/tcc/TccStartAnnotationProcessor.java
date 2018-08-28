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
package org.apache.servicecomb.saga.omega.transaction.tcc;

import javax.transaction.TransactionalException;

import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;

public class TccStartAnnotationProcessor {

  private final OmegaContext omegaContext;
  private final TccEventService eventService;

  TccStartAnnotationProcessor(OmegaContext omegaContext, TccEventService eventService) {
    this.omegaContext = omegaContext;
    this.eventService = eventService;
  }

  public AlphaResponse preIntercept(String parentTxId, String methodName, int timeout) {
    try {
      return eventService.tccTransactionStart(new TccStartedEvent(omegaContext.globalTxId(), omegaContext.localTxId()));
    } catch (OmegaException e) {
      throw new TransactionalException(e.getMessage(), e.getCause());
    }
  }

  public void postIntercept(String parentTxId, String methodName) {
    eventService.tccTransactionStop(new TccEndedEvent(omegaContext.globalTxId(), omegaContext.localTxId(),
        TransactionStatus.Succeed));
  }

  public void onError(String parentTxId, String methodName, Throwable throwable) {
    // Send the cancel event
    // Do we need to wait for the alpha finish all the transaction
    eventService.tccTransactionStop(new TccEndedEvent(omegaContext.globalTxId(), omegaContext.localTxId(),
        TransactionStatus.Failed));
  }
}
