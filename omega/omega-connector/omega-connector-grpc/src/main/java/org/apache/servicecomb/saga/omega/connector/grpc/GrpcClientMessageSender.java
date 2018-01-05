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
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent.Builder;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceStub;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;

public class GrpcClientMessageSender implements MessageSender {

  private final TxEventServiceStub asyncEventService;

  private final MessageSerializer serializer;

  private final TxEventServiceBlockingStub blockingEventService;
  private final GrpcCompensateStreamObserver compensateStreamObserver;
  private final GrpcServiceConfig serviceConfig;

  public GrpcClientMessageSender(ManagedChannel channel,
      MessageSerializer serializer,
      MessageDeserializer deserializer,
      ServiceConfig serviceConfig,
      MessageHandler handler) {
    this.asyncEventService = TxEventServiceGrpc.newStub(channel);
    this.blockingEventService = TxEventServiceGrpc.newBlockingStub(channel);
    this.serializer = serializer;

    this.compensateStreamObserver = new GrpcCompensateStreamObserver(handler, deserializer);
    this.serviceConfig = serviceConfig(serviceConfig.serviceName(), serviceConfig.instanceId());
  }

  @Override
  public void onConnected() {
    asyncEventService.onConnected(serviceConfig, compensateStreamObserver);
  }

  @Override
  public void onDisconnected() {
    blockingEventService.onDisconnected(serviceConfig);
  }

  @Override
  public void send(TxEvent event) {
    blockingEventService.onTxEvent(convertEvent(event));
  }

  private GrpcTxEvent convertEvent(TxEvent event) {
    ByteString payloads = ByteString.copyFrom(serializer.serialize(event.payloads()));

    Builder builder = GrpcTxEvent.newBuilder()
        .setServiceName(serviceConfig.getServiceName())
        .setInstanceId(serviceConfig.getInstanceId())
        .setTimestamp(event.timestamp())
        .setGlobalTxId(event.globalTxId())
        .setLocalTxId(event.localTxId())
        .setType(event.type())
        .setCompensationMethod(event.compensationMethod())
        .setPayloads(payloads);

    if (event.parentTxId() != null) {
      builder.setParentTxId(event.parentTxId());
    }
    return builder.build();
  }

  private GrpcServiceConfig serviceConfig(String serviceName, String instanceId) {
    return GrpcServiceConfig.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .build();
  }
}
