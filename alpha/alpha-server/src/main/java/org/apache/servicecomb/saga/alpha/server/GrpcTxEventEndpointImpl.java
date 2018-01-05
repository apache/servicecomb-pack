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

package org.apache.servicecomb.saga.alpha.server;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;

import io.grpc.stub.StreamObserver;

class GrpcTxEventEndpointImpl extends TxEventServiceImplBase {

  private static final GrpcAck ACK = GrpcAck.newBuilder().build();
  private final TxConsistentService txConsistentService;

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  GrpcTxEventEndpointImpl(TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks) {
    this.txConsistentService = txConsistentService;
    this.omegaCallbacks = omegaCallbacks;
  }

  @Override
  public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcCompensateCommand> responseObserver) {
    omegaCallbacks
        .computeIfAbsent(request.getServiceName(), key -> new ConcurrentHashMap<>())
        .computeIfAbsent(request.getInstanceId(), key -> new GrpcOmegaCallback(responseObserver));
  }

  @Override
  public void onTxEvent(GrpcTxEvent message, StreamObserver<GrpcAck> responseObserver) {
    txConsistentService.handle(new TxEvent(
        message.getServiceName(),
        message.getInstanceId(),
        new Date(message.getTimestamp()),
        message.getGlobalTxId(),
        message.getLocalTxId(),
        message.getParentTxId().isEmpty() ? null : message.getParentTxId(),
        message.getType(),
        message.getCompensationMethod(),
        message.getPayloads().toByteArray()
    ));

    responseObserver.onNext(ACK);
    responseObserver.onCompleted();
  }
}
