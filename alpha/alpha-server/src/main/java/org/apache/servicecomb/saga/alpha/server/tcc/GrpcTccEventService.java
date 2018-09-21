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
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.OmegaCallback;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.OmegaCallbacksRegistry;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.EventConverter;
import org.apache.servicecomb.saga.alpha.server.tcc.service.TccTxEventService;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Grpc TCC event service implement.
 */
public class GrpcTccEventService extends TccEventServiceGrpc.TccEventServiceImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final GrpcAck ALLOW = GrpcAck.newBuilder().setAborted(false).build();
  
  private static final GrpcAck REJECT = GrpcAck.newBuilder().setAborted(true).build();

  private final TccTxEventService tccTxEventService;

  public GrpcTccEventService(TccTxEventService tccTxEventService) {
    this.tccTxEventService = tccTxEventService;
  }

  @Override
  public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcTccCoordinateCommand> responseObserver) {
    OmegaCallbacksRegistry.register(request, responseObserver);
    LOG.info("Established connection service [{}] instanceId [{}].", request.getServiceName(), request.getInstanceId());
  }

  @Override
  public void onTccTransactionStarted(GrpcTccTransactionStartedEvent request, StreamObserver<GrpcAck> responseObserver) {
    LOG.info("Received transaction start event, global tx id: {}", request.getGlobalTxId());
    responseObserver.onNext(tccTxEventService.onTccStartedEvent(EventConverter.convertToGlobalTxEvent(request)) ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }

  @Override
  public void participate(GrpcTccParticipatedEvent request, StreamObserver<GrpcAck> responseObserver) {
    LOG.info("Received participated event from service {} , global tx id: {}, local tx id: {}", request.getServiceName(),
        request.getGlobalTxId(), request.getLocalTxId()) ;
    responseObserver.onNext(
        tccTxEventService.onParticipatedEvent(EventConverter.convertToParticipatedEvent(request)) ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }

  @Override
  public void onTccTransactionEnded(GrpcTccTransactionEndedEvent request, StreamObserver<GrpcAck> responseObserver) {
    LOG.info("Received transaction end event, global tx id: {}", request.getGlobalTxId());
    responseObserver.onNext(tccTxEventService.onTccEndedEvent(EventConverter.convertToGlobalTxEvent(request)) ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }

  @Override
  public void onTccCoordinated(GrpcTccCoordinatedEvent request, StreamObserver<GrpcAck> responseObserver) {
    LOG.info("Received coordinated event, global tx: {}, local tx: {}, parent id: {}, "
            + "method: {}, status: {}, service [{}] instanceId [{}]",
        request.getGlobalTxId(), request.getLocalTxId(), request.getParentTxId(),
        request.getMethodName(), request.getStatus(), request.getServiceName(), request.getInstanceId());
    responseObserver.onNext(tccTxEventService.onCoordinatedEvent(EventConverter.convertToTccTxEvent(request)) ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }
  
  @Override
  public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
    OmegaCallback omegaCallback = OmegaCallbacksRegistry.retrieveThenRemove(request.getServiceName(), request.getInstanceId());
    if (null != omegaCallback) {
      LOG.info("Disconnect from alpha, service [{}] instanceId [{}].", request.getServiceName(), request.getInstanceId());
      omegaCallback.disconnect();
    }
    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }
}
