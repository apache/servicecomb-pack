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

package org.apache.servicecomb.pack.omega.connector.grpc.saga;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.ReconnectStreamObserver;
import org.apache.servicecomb.pack.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcCompensateStreamObserver extends ReconnectStreamObserver<GrpcCompensateCommand> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public GrpcCompensateStreamObserver(LoadBalanceContext loadContext,
      MessageSender messageSender,
      MessageHandler messageHandler, MessageDeserializer deserializer) {
    super(loadContext, messageSender);
    this.messageHandler = messageHandler;
    this.deserializer = deserializer;
  }

  private final MessageHandler messageHandler;
  private final MessageDeserializer deserializer;


  @Override
  public void onNext(GrpcCompensateCommand command) {
    LOG.info("Received compensate command, global tx id: {}, local tx id: {}, compensation method: {}",
        command.getGlobalTxId(), command.getLocalTxId(), command.getCompensationMethod());

    messageHandler.onReceive(
        command.getGlobalTxId(),
        command.getLocalTxId(),
        command.getParentTxId().isEmpty() ? null : command.getParentTxId(),
        command.getCompensationMethod(),
        deserializer.deserialize(command.getPayloads().toByteArray()));
  }
}
