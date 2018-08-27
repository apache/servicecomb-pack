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

package org.apache.servicecomb.saga.alpha.server.tcc;

import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.core.AlphaException;
import org.apache.servicecomb.saga.alpha.server.tcc.event.ParticipateEventFactory;
import org.apache.servicecomb.saga.alpha.server.tcc.event.ParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;

/**
 * Grpc TCC event service implement.
 */
public class GrpcTccEventService extends TccEventServiceGrpc.TccEventServiceImplBase {

  private static final GrpcAck ALLOW = GrpcAck.newBuilder().setAborted(false).build();
  private static final GrpcAck REJECT = GrpcAck.newBuilder().setAborted(true).build();

  @Override
  public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcTccCoordinateCommand> responseObserver) {
    OmegaCallbacksRegistry.register(request, responseObserver);
  }

  @Override
  public void onTccTransactionStarted(GrpcTccTransactionStartedEvent request, StreamObserver<GrpcAck> responseObserver) {
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void participate(GrpcTccParticipatedEvent request, StreamObserver<GrpcAck> responseObserver) {
    TransactionEventRegistry.register(ParticipateEventFactory.create(request));
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void onTccTransactionEnded(GrpcTccTransactionEndedEvent request, StreamObserver<GrpcAck> responseObserver) {
    try {
      for (ParticipatedEvent event : TransactionEventRegistry.retrieve(request.getGlobalTxId())) {
        OmegaCallbacksRegistry.retrieve(event.getServiceName(), event.getInstanceId())
            .invoke(event, request.getStatus());
      }
    } catch (AlphaException ex) {
      responseObserver.onNext(REJECT);
    }
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
    OmegaCallback omegaCallback = OmegaCallbacksRegistry.retrieveThenRemove(request.getServiceName(), request.getInstanceId());
    if (null != omegaCallback) {
      omegaCallback.disconnect();
    }
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }
}
