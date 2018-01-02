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

import static org.apache.servicecomb.saga.alpha.core.EventType.TxStartedEvent;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;

class GrpcTxEventStreamObserver implements StreamObserver<GrpcTxEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  private final TxConsistentService txConsistentService;

  private final StreamObserver<GrpcCompensateCommand> responseObserver;

  GrpcTxEventStreamObserver(Map<String, Map<String, OmegaCallback>> omegaCallbacks,
      TxConsistentService txConsistentService, StreamObserver<GrpcCompensateCommand> responseObserver) {
    this.omegaCallbacks = omegaCallbacks;
    this.txConsistentService = txConsistentService;
    this.responseObserver = responseObserver;
  }

  @Override
  public void onNext(GrpcTxEvent message) {
    // register a callback on started event
    if (message.getType().equals(TxStartedEvent.name())) {
      omegaCallbacks.computeIfAbsent(message.getServiceName(), (key) -> new ConcurrentHashMap<>())
          .put(message.getInstanceId(), new GrpcOmegaCallback(responseObserver));
    }

    // store received event
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
  }

  @Override
  public void onError(Throwable t) {
    LOG.error("failed to process grpc message.", t);
    responseObserver.onCompleted();
  }

  @Override
  public void onCompleted() {
    responseObserver.onCompleted();
  }
}
