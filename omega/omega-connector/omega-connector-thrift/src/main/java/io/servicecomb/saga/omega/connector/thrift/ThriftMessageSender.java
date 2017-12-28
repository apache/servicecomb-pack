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

package io.servicecomb.saga.omega.connector.thrift;

import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.MessageSerializer;
import io.servicecomb.saga.omega.transaction.TxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEventEndpoint;

public class ThriftMessageSender implements MessageSender, AutoCloseable {
  private final SwiftTxEventEndpoint eventService;
  private final MessageSerializer serializer;

  public ThriftMessageSender(SwiftTxEventEndpoint eventService, MessageSerializer serializer) {
    this.eventService = eventService;
    this.serializer = serializer;
  }

  @Override
  public void send(TxEvent event) {
    eventService.handle(new SwiftTxEvent(
        event.timestamp(),
        event.globalTxId(),
        event.localTxId(),
        event.parentTxId(),
        event.type(),
        event.compensationMethod(),
        serializer.serialize(event)
    ));
  }

  @Override
  public void close() throws Exception {
    eventService.close();
  }
}
