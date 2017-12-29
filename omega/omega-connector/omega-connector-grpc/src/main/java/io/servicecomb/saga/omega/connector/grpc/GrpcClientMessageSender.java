/*
 *
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
 *
 *
 */

package io.servicecomb.saga.omega.connector.grpc;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.servicecomb.saga.omega.transaction.MessageSender;
import io.servicecomb.saga.omega.transaction.MessageSerializer;
import io.servicecomb.saga.omega.transaction.TxEvent;
import io.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import io.servicecomb.saga.pack.contract.grpc.GrpcTxEvent.Builder;
import io.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc;
import io.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceBlockingStub;

public class GrpcClientMessageSender implements MessageSender {

  private final TxEventServiceBlockingStub eventService;

  private final MessageSerializer serializer;

  public GrpcClientMessageSender(ManagedChannel eventService, MessageSerializer serializer) {
    this.eventService = TxEventServiceGrpc.newBlockingStub(eventService);
    this.serializer = serializer;
  }

  @Override
  public void send(TxEvent event) {
    eventService.reportEvent(convertEvent(event));
  }

  private GrpcTxEvent convertEvent(TxEvent event) {
    ByteString payloads = ByteString.copyFrom(serializer.serialize(event.payloads()));

    Builder builder = GrpcTxEvent.newBuilder()
        .setTimestamp(event.timestamp())
        .setGlobalTxId(event.globalTxId())
        .setLocalTxId(event.localTxId())
        .setType(event.type())
        .setPayloads(payloads);

    if (event.parentTxId() != null) {
      builder.setParentTxId(event.parentTxId());
    }
    return builder.build();
  }
}
