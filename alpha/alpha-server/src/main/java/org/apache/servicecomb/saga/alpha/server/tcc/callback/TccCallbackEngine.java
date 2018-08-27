/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server.tcc.callback;

import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.saga.alpha.server.tcc.registry.TransactionEventRegistry;
import org.apache.servicecomb.saga.alpha.server.tcc.event.ParticipatedEvent;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TccCallbackEngine implements CallbackEngine {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final OmegaCallbackWrapper omegaCallbackWrapper;

  public TccCallbackEngine(OmegaCallbackWrapper omegaCallbackWrapper) {
    this.omegaCallbackWrapper = omegaCallbackWrapper;
  }

  @Override
  public boolean execute(GrpcTccTransactionEndedEvent request) {
    boolean result = true;
    for (ParticipatedEvent event : TransactionEventRegistry.retrieve(request.getGlobalTxId())) {
      try {
        omegaCallbackWrapper.invoke(event, TransactionStatus.valueOf(request.getStatus()));
      } catch (Exception ex) {
        logError(event, ex);
        result = false;
      }
    }
    return result;
  }

  private void logError(ParticipatedEvent event, Exception ex) {
    LOG.error(
        "Failed to invoke service [{}] instance [{}] with method [{}], global tx id [{}] and local tx id [{}]",
        event.getServiceName(),
        event.getInstanceId(),
        TransactionStatus.Succeed.equals(event.getStatus()) ? event.getConfirmMethod() : event.getCancelMethod(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        ex);
  }
}
