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

package io.servicecomb.saga.alpha.server;

import java.util.Date;

import io.servicecomb.saga.alpha.core.TxConsistentService;
import io.servicecomb.saga.alpha.core.TxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEventEndpoint;

class SwiftTxEventEndpointImpl implements SwiftTxEventEndpoint {

  private final TxConsistentService txConsistentService;

  SwiftTxEventEndpointImpl(TxConsistentService txConsistentService) {
    this.txConsistentService = txConsistentService;
  }

  @Override
  public void handle(SwiftTxEvent message) {
    txConsistentService.handle(new TxEvent(
        new Date(message.timestamp()),
        message.globalTxId(),
        message.localTxId(),
        message.parentTxId(),
        message.type(),
        message.compensationMethod(),
        message.payloads()
    ));
  }

  @Override
  public void close() throws Exception {
    
  }
}
