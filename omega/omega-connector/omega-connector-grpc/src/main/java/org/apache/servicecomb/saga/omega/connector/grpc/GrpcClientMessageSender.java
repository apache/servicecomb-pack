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

package org.apache.servicecomb.saga.omega.connector.grpc;

import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent.Builder;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceStub;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class GrpcClientMessageSender implements MessageSender {

  private final TxEventServiceStub eventService;

  private final MessageSerializer serializer;
  private final ServiceConfig serviceConfig;

  private StreamObserver<GrpcTxEvent> requestObserver;

  public GrpcClientMessageSender(ManagedChannel channel, MessageSerializer serializer, ServiceConfig serviceConfig,
      MessageHandler handler) {
    this.eventService = TxEventServiceGrpc.newStub(channel);
    this.serializer = serializer;
    this.serviceConfig = serviceConfig;
    this.requestObserver = this.eventService.callbackCommand(new GrpcCompensateStreamObserver(handler));
  }

  @Override
  public void send(TxEvent event) {
    requestObserver.onNext(convertEvent(event));
  }

  private GrpcTxEvent convertEvent(TxEvent event) {
    ByteString payloads = ByteString.copyFrom(serializer.serialize(event.payloads()));

    Builder builder = GrpcTxEvent.newBuilder()
        .setServiceName(serviceConfig.serviceName())
        .setInstanceId(serviceConfig.instanceId())
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
