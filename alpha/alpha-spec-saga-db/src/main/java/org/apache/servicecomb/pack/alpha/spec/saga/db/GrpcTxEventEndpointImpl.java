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

package org.apache.servicecomb.pack.alpha.spec.saga.db;

import static java.util.Collections.emptyMap;

import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.TxConsistentService;
import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcTxEventEndpointImpl extends TxEventServiceImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final GrpcAck ALLOW = GrpcAck.newBuilder().setAborted(false).build();
  private static final GrpcAck REJECT = GrpcAck.newBuilder().setAborted(true).build();

  private final TxConsistentService txConsistentService;

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;
  private final ServerMeta serverMeta;

  GrpcTxEventEndpointImpl(TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks, ServerMeta serverMeta) {
    this.txConsistentService = txConsistentService;
    this.omegaCallbacks = omegaCallbacks;
    this.serverMeta = serverMeta;
  }

  @Override
  public StreamObserver<GrpcServiceConfig> onConnected(StreamObserver<GrpcCompensateCommand> responseObserver) {
    return new StreamObserver<GrpcServiceConfig>() {
      @Override
      public void onNext(GrpcServiceConfig grpcServiceConfig) {
        omegaCallbacks
            .computeIfAbsent(grpcServiceConfig.getServiceName(), key -> new ConcurrentHashMap<>())
            .put(grpcServiceConfig.getInstanceId(), new GrpcOmegaCallback(responseObserver));
      }

      @Override
      public void onError(Throwable throwable) {
        LOG.error(throwable.getMessage());
      }

      @Override
      public void onCompleted() {
        LOG.info("Omega client called method onCompleted of GrpcServiceConfig");
      }
    };
  }

  // TODO: 2018/1/5 connect is async and disconnect is sync, meaning callback may not be registered on disconnected
  @Override
  public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
    OmegaCallback callback = omegaCallbacks.getOrDefault(request.getServiceName(), emptyMap())
        .remove(request.getInstanceId());

    if (callback != null) {
      callback.disconnect();
    }

    responseObserver.onNext(ALLOW);
    responseObserver.onCompleted();
  }

  @Override
  public void onTxEvent(GrpcTxEvent message, StreamObserver<GrpcAck> responseObserver) {
    boolean ok = txConsistentService.handle(new TxEvent(
        message.getServiceName(),
        message.getInstanceId(),
        new Date(),
        message.getGlobalTxId(),
        message.getLocalTxId(),
        message.getParentTxId().isEmpty() ? null : message.getParentTxId(),
        message.getType(),
        message.getCompensationMethod(),
        message.getTimeout(),
        message.getRetryMethod(),
        message.getForwardRetries(),
        message.getPayloads().toByteArray()
    ));

    responseObserver.onNext(ok ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }

  @Override
  public void onGetServerMeta(GrpcServiceConfig request, StreamObserver<ServerMeta> responseObserver){
    responseObserver.onNext(this.serverMeta);
    responseObserver.onCompleted();
  }
}
