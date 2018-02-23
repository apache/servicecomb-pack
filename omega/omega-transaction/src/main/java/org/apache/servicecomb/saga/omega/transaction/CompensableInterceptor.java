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

import org.apache.servicecomb.saga.omega.context.OmegaContext;

class CompensableInterceptor implements EventAwareInterceptor {
  private final OmegaContext context;
  private final MessageSender sender;

  CompensableInterceptor(OmegaContext context, MessageSender sender) {
    this.sender = sender;
    this.context = context;
  }

  @Override
  public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
      int retries, Object... message) {
    return sender.send(new TxStartedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod,
        timeout, retriesMethod, retries, message));
  }

  @Override
  public void postIntercept(String parentTxId, String compensationMethod) {
    sender.send(new TxEndedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod));
  }

  @Override
  public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
    sender.send(
        new TxAbortedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod, throwable));
  }
}
