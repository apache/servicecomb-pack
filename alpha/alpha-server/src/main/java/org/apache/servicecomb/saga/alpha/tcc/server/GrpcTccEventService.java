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

package org.apache.servicecomb.saga.alpha.tcc.server;

import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.tcc.server.event.ParticipateEvent;
import org.apache.servicecomb.saga.alpha.tcc.server.event.ParticipateEventFactory;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipateEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;

/**
 * Grpc TCC event service implement.
 *
 * @author zhaojun
 */
public class GrpcTccEventService extends TccEventServiceGrpc.TccEventServiceImplBase {

  private static final GrpcAck ALLOW = GrpcAck.newBuilder().setAborted(false).build();
  private static final GrpcAck REJECT = GrpcAck.newBuilder().setAborted(true).build();

  @Override
  public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcTccCordinateCommand> responseObserver) {
    OmegaCallbacksRegistry.register(request, responseObserver);
  }

  @Override
  public void onTccTransactionStarted(GrpcTccTransactionStartedEvent request, StreamObserver<GrpcAck> responseObserver) {
  }

  @Override
  public void participate(GrpcTccParticipateEvent request, StreamObserver<GrpcAck> responseObserver) {
    TransactionEventRegistry.register(ParticipateEventFactory.create(request));
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void onTccTransactionEnded(GrpcTccTransactionEndedEvent request, StreamObserver<GrpcAck> responseObserver) {
    for (ParticipateEvent event : TransactionEventRegistry.retrieve(request.getGlobalTxId())) {
      OmegaCallbacksRegistry.retrieve(event.getServiceName(), event.getInstanceId()).compensate(event, event.getStatus());
    }
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
    OmegaCallbacksRegistry.retrieveThenRemove(request.getServiceName(), request.getInstanceId()).disconnect();
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }
}
