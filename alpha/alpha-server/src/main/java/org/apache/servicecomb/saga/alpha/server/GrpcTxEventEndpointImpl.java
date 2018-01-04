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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;

import io.grpc.stub.StreamObserver;

class GrpcTxEventEndpointImpl extends TxEventServiceImplBase {

  private final TxConsistentService txConsistentService;

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  private final Map<StreamObserver<GrpcCompensateCommand>, SimpleImmutableEntry<String, String>> omegaCallbacksReverse;

  GrpcTxEventEndpointImpl(TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks,
      Map<StreamObserver<GrpcCompensateCommand>, SimpleImmutableEntry<String, String>> omegaCallbacksReverse) {
    this.txConsistentService = txConsistentService;
    this.omegaCallbacks = omegaCallbacks;
    this.omegaCallbacksReverse = omegaCallbacksReverse;
  }

  @Override
  public StreamObserver<GrpcTxEvent> callbackCommand(StreamObserver<GrpcCompensateCommand> responseObserver) {
    return new GrpcTxEventStreamObserver(omegaCallbacks, omegaCallbacksReverse, txConsistentService, responseObserver);
  }
}
