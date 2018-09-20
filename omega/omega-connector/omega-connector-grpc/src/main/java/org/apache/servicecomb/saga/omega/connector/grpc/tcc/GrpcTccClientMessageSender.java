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

package org.apache.servicecomb.saga.omega.connector.grpc.tcc;

import io.grpc.ManagedChannel;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceStub;

public class GrpcTccClientMessageSender implements TccMessageSender {

  private final GrpcServiceConfig serviceConfig;
  private final String target;
  private final TccEventServiceBlockingStub tccBlockingEventService;
  private final TccEventServiceStub tccAsyncEventService;
  private final GrpcCoordinateStreamObserver observer;

  public GrpcTccClientMessageSender(ServiceConfig serviceConfig,
      ManagedChannel channel,
      String address,
      TccMessageHandler handler,
      LoadBalanceContext loadContext) {
    this.target = address;
    tccBlockingEventService = TccEventServiceGrpc.newBlockingStub(channel);
    tccAsyncEventService = TccEventServiceGrpc.newStub(channel);
    this.serviceConfig = serviceConfig(serviceConfig.serviceName(), serviceConfig.instanceId());
    observer = new GrpcCoordinateStreamObserver(loadContext, this, handler);
  }

  @Override
  public void onConnected() {
    tccAsyncEventService.onConnected(serviceConfig, observer);
  }

  @Override
  public void onDisconnected() {
    tccBlockingEventService.onDisconnected(serviceConfig);
  }

  @Override
  public void close() {
    // do nothing here
  }

  @Override
  public String target() {
    return target;
  }

  @Override
  public AlphaResponse participate(ParticipatedEvent participateEvent) {
    GrpcAck grpcAck = tccBlockingEventService.participate(convertTo(participateEvent));
    return new AlphaResponse(grpcAck.getAborted());
  }

  @Override
  public AlphaResponse tccTransactionStart(TccStartedEvent tccStartEvent) {
    GrpcAck grpcAck = tccBlockingEventService.onTccTransactionStarted(convertTo(tccStartEvent));
    return new AlphaResponse(grpcAck.getAborted());
  }


  @Override
  public AlphaResponse tccTransactionStop(TccEndedEvent tccEndEvent) {
    GrpcAck grpcAck = tccBlockingEventService.onTccTransactionEnded(convertTo(tccEndEvent));
    return new AlphaResponse(grpcAck.getAborted());

  }

  @Override
  public AlphaResponse coordinate(CoordinatedEvent coordinatedEvent) {
    GrpcAck grpcAck = tccBlockingEventService.onTccCoordinated(convertTo(coordinatedEvent));
    return new AlphaResponse(grpcAck.getAborted());
  }

  private GrpcTccCoordinatedEvent convertTo(CoordinatedEvent coordinatedEvent) {
    return GrpcTccCoordinatedEvent.newBuilder()
        .setServiceName(serviceConfig.getServiceName())
        .setInstanceId(serviceConfig.getInstanceId())
        .setGlobalTxId(coordinatedEvent.getGlobalTxId())
        .setLocalTxId(coordinatedEvent.getLocalTxId())
        .setParentTxId(coordinatedEvent.getParentTxId())
        .setMethodName(coordinatedEvent.getMethodName())
        .setStatus(coordinatedEvent.getStatus().toString())
        .build();
  }

  private GrpcServiceConfig serviceConfig(String serviceName, String instanceId) {
    return GrpcServiceConfig.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .build();
  }

  private GrpcTccTransactionStartedEvent convertTo(TccStartedEvent tccStartEvent) {
    return GrpcTccTransactionStartedEvent.newBuilder()
        .setServiceName(serviceConfig.getServiceName())
        .setInstanceId(serviceConfig.getInstanceId())
        .setGlobalTxId(tccStartEvent.getGlobalTxId())
        .setLocalTxId(tccStartEvent.getLocalTxId())
        .build();
  }

  private GrpcTccTransactionEndedEvent convertTo(TccEndedEvent tccEndEvent) {
    return GrpcTccTransactionEndedEvent.newBuilder()
        .setServiceName(serviceConfig.getServiceName())
        .setInstanceId(serviceConfig.getInstanceId())
        .setGlobalTxId(tccEndEvent.getGlobalTxId())
        .setLocalTxId(tccEndEvent.getLocalTxId())
        .setStatus(tccEndEvent.getStatus().toString())
        .build();
  }

  private GrpcTccParticipatedEvent convertTo(ParticipatedEvent participateEvent) {
    return GrpcTccParticipatedEvent.newBuilder()
        .setServiceName(serviceConfig.getServiceName())
        .setInstanceId(serviceConfig.getInstanceId())
        .setGlobalTxId(participateEvent.getGlobalTxId())
        .setLocalTxId(participateEvent.getLocalTxId())
        .setParentTxId(participateEvent.getParentTxId())
        .setCancelMethod(participateEvent.getCancelMethod())
        .setConfirmMethod(participateEvent.getConfirmMethod())
        .setStatus(participateEvent.getStatus().toString())
        .build();
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    return null;
  }
}
