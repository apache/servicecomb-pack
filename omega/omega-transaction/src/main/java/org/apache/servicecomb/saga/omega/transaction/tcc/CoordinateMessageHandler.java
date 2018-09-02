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

import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;

public class CoordinateMessageHandler implements MessageHandler {

  private final TccEventService tccEventService;

  private final CallbackContext callbackContext;

  private final OmegaContext omegaContext;

  private final ParametersContext parametersContext;

  public CoordinateMessageHandler(TccEventService tccEventService,
      CallbackContext callbackContext, OmegaContext omegaContext,
      ParametersContext parametersContext) {
    this.tccEventService = tccEventService;
    this.callbackContext = callbackContext;
    this.omegaContext = omegaContext;
    this.parametersContext = parametersContext;
  }

  @Override
  public void onReceive(String globalTxId, String localTxId, String parentTxId, String methodName) {
    // TODO need to catch the exception and send the failed message
    // The parameter need to be updated here
    callbackContext.apply(globalTxId, localTxId, methodName, parametersContext.getParameters(localTxId));
    tccEventService.coordinate(new CoordinatedEvent(globalTxId, localTxId, parentTxId, methodName, TransactionStatus.Succeed));
    // Need to remove the parameter
    parametersContext.removeParameter(localTxId);
  }
}
