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

package org.apache.servicecomb.saga.alpha.server.tcc.callback;

import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;

/**
 * Grpc omega callback for TCC workflow.
 */
public final class GrpcOmegaTccCallback implements OmegaCallback {

  private StreamObserver<GrpcTccCoordinateCommand> responseObserver;

  public GrpcOmegaTccCallback(StreamObserver<GrpcTccCoordinateCommand> responseObserver) {
    this.responseObserver = responseObserver;
  }

  @Override
  public void invoke(ParticipatedEvent event, TransactionStatus status) {
    GrpcTccCoordinateCommand command = GrpcTccCoordinateCommand.newBuilder()
        .setGlobalTxId(event.getGlobalTxId())
        .setLocalTxId(event.getLocalTxId())
        .setParentTxId(event.getParentTxId() == null ? "" : event.getParentTxId())
        .setServiceName(event.getServiceName())
        .setMethod(TransactionStatus.Succeed.equals(status) ? event.getConfirmMethod() : event.getCancelMethod())
        .build();
    responseObserver.onNext(command);
  }

  @Override
  public void disconnect() {
    responseObserver.onCompleted();
  }
}
