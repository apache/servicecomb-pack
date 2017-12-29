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

package io.servicecomb.saga.alpha.server;

import java.util.Date;

import io.grpc.stub.StreamObserver;
import io.servicecomb.saga.alpha.core.TxConsistentService;
import io.servicecomb.saga.alpha.core.TxEvent;
import io.servicecomb.saga.pack.contract.grpc.GrpcEmpty;
import io.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import io.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;

class GrpcTxEventEndpointImpl extends TxEventServiceImplBase {

  private final TxConsistentService txConsistentService;

  GrpcTxEventEndpointImpl(TxConsistentService txConsistentService) {
    this.txConsistentService = txConsistentService;
  }

  @Override
  public void reportEvent(GrpcTxEvent message, StreamObserver<GrpcEmpty> responseObserver) {
    txConsistentService.handle(new TxEvent(
        message.getServiceName(),
        message.getInstanceId(),
        new Date(message.getTimestamp()),
        message.getGlobalTxId(),
        message.getLocalTxId(),
        message.getParentTxId().isEmpty()? null : message.getParentTxId(),
        message.getType(),
        message.getCompensationMethod(),
        message.getPayloads().toByteArray()
    ));

    GrpcEmpty reply = GrpcEmpty.newBuilder().build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
