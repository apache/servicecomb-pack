/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.servicecomb.saga.alpha.tcc.server;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.tcc.server.event.ParticipateEvent;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCordinateCommand;

/**
 * Grpc omega callback for TCC workflow.
 *
 * @author zhaojun
 */
public final class GrpcOmegaTccCallback implements OmegaCallback<ParticipateEvent> {

  private StreamObserver<GrpcTccCordinateCommand> responseObserver;

  public GrpcOmegaTccCallback(StreamObserver<GrpcTccCordinateCommand> responseObserver) {
    this.responseObserver = responseObserver;
  }

  @Override
  public void execute(ParticipateEvent event, TransactionStatus status) {
    GrpcTccCordinateCommand command = GrpcTccCordinateCommand.newBuilder()
        .setGlobalTxId(event.getGlobalTxId())
        .setLocalTxId(event.getLocalTxId())
        .setParentTxId(event.getParentTxId() == null ? "" : event.getParentTxId())
        .setMethod(TransactionStatus.Succeed.equals(status) ? event.getConfirmMethod() : event.getCancelMethod())
        .build();
    responseObserver.onNext(command);
  }
}
