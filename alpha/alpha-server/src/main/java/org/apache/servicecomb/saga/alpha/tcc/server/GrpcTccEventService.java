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

import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCoordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcParticipateEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;

/**
 * Grpc TCC event service implement.
 *
 * @author zhaojun
 */
public class GrpcTccEventService extends TccEventServiceGrpc.TccEventServiceImplBase {

  @Override
  public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcCoordinateCommand> responseObserver) {
  }

  @Override
  public void onTransactionStarted(GrpcTransactionStartedEvent request, StreamObserver<GrpcAck> responseObserver) {
  }

  @Override
  public void participate(GrpcParticipateEvent request, StreamObserver<GrpcAck> responseObserver) {
  }

  @Override
  public void onTransactionEnded(GrpcTransactionEndedEvent request, StreamObserver<GrpcAck> responseObserver) {
  }

  @Override
  public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
  }
}
