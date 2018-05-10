/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transport.servicecomb;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;

public class SagaProviderHandler implements Handler {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final OmegaContext omegaContext;

  public SagaProviderHandler(@Autowired(required=false) OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
    if (omegaContext == null) {
      LOG.info("The OmegaContext is not injected, The SagaProviderHander is disabled.");
    }
  }

  @Override
  public void handle(Invocation invocation, AsyncResponse asyncResponse) throws Exception {
    if (omegaContext != null) {
      String globalTxId = invocation.getContext().get(GLOBAL_TX_ID_KEY);
      if (globalTxId == null) {
        LOG.debug("no such header: {}", GLOBAL_TX_ID_KEY);
      } else {

        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(invocation.getContext().get(LOCAL_TX_ID_KEY));
      }
    }

    invocation.next(asyncResponse);
  }
}
