/*
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
 */

package org.apache.servicecomb.pack.alpha.spec.saga.akka;

import static java.util.Collections.emptyMap;

import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.fsm.CompensateAckType;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckFailedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckSucceedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.common.EventType;
import org.apache.servicecomb.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcSagaEventService extends TxEventServiceImplBase {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final GrpcAck ALLOW = GrpcAck.newBuilder().setAborted(false).build();
  private static final GrpcAck REJECT = GrpcAck.newBuilder().setAborted(true).build();

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;
  private final ActorEventChannel actorEventChannel;
  private final ServerMeta serverMeta;

  public GrpcSagaEventService(ActorEventChannel actorEventChannel,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks, ServerMeta serverMeta) {
    this.actorEventChannel = actorEventChannel;
    this.omegaCallbacks = omegaCallbacks;
    this.serverMeta = serverMeta;
  }

  @Override
  public StreamObserver<GrpcServiceConfig> onConnected(StreamObserver<GrpcCompensateCommand> responseObserver) {
    return new StreamObserver<GrpcServiceConfig>(){

      private GrpcOmegaCallback grpcOmegaCallback;

      @Override
      public void onNext(GrpcServiceConfig grpcServiceConfig) {
        grpcOmegaCallback = new GrpcOmegaCallback(responseObserver);
        omegaCallbacks
            .computeIfAbsent(grpcServiceConfig.getServiceName(), key -> new ConcurrentHashMap<>())
            .put(grpcServiceConfig.getInstanceId(), grpcOmegaCallback);
      }

      @Override
      public void onError(Throwable throwable) {
        LOG.error(throwable.getMessage());
      }

      @Override
      public void onCompleted() {
        // Do nothing here
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
    if(LOG.isDebugEnabled()){
      LOG.debug("onText {}",message);
    }
    boolean ok = true;
    BaseEvent event = null;
    if (message.getType().equals(EventType.SagaStartedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .createTime(new Date())
          .timeout(message.getTimeout()).build();
    } else if (message.getType().equals(EventType.SagaEndedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .createTime(new Date())
          .globalTxId(message.getGlobalTxId()).build();
    } else if (message.getType().equals(EventType.SagaAbortedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.SagaAbortedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .createTime(new Date())
          .payloads(message.getPayloads().toByteArray()).build();
    } else if (message.getType().equals(EventType.SagaTimeoutEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.SagaTimeoutEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .createTime(new Date())
          .globalTxId(message.getGlobalTxId()).build();
    } else if (message.getType().equals(EventType.TxStartedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .localTxId(message.getLocalTxId())
          .parentTxId(message.getParentTxId().isEmpty() ? null : message.getParentTxId())
          .compensationMethod(message.getCompensationMethod())
          .retryMethod(message.getRetryMethod())
          .forwardRetries(message.getForwardRetries())
          .forwardTimeout(message.getForwardTimeout())
          .reverseRetries(message.getReverseRetries())
          .reverseTimeout(message.getReverseTimeout())
          .retryDelayInMilliseconds(message.getRetryDelayInMilliseconds())
          .createTime(new Date())
          .payloads(message.getPayloads().toByteArray()).build();
    } else if (message.getType().equals(EventType.TxEndedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .parentTxId(message.getParentTxId())
          .localTxId(message.getLocalTxId()).build();
    } else if (message.getType().equals(EventType.TxAbortedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .parentTxId(message.getParentTxId())
          .localTxId(message.getLocalTxId())
          .createTime(new Date())
          .payloads(message.getPayloads().toByteArray()).build();
    } else if (message.getType().equals(EventType.TxCompensatedEvent.name())) {
      event = org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensatedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .parentTxId(message.getParentTxId())
          .createTime(new Date())
          .localTxId(message.getLocalTxId()).build();
    } else if (message.getType().equals(EventType.TxCompensateAckSucceedEvent.name())) {
      event = TxCompensateAckSucceedEvent.builder()
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .parentTxId(message.getParentTxId())
          .createTime(new Date())
          .localTxId(message.getLocalTxId()).build();
      omegaCallbacks.get(message.getServiceName()).get(message.getInstanceId())
          .getAck(CompensateAckType.Succeed);
    } else if (message.getType().equals(EventType.TxCompensateAckFailedEvent.name())) {
      event = TxCompensateAckFailedEvent.builder()
          .payloads(message.getPayloads().toByteArray())
          .serviceName(message.getServiceName())
          .instanceId(message.getInstanceId())
          .globalTxId(message.getGlobalTxId())
          .parentTxId(message.getParentTxId())
          .createTime(new Date())
          .localTxId(message.getLocalTxId()).build();
      omegaCallbacks.get(message.getServiceName()).get(message.getInstanceId())
          .getAck(CompensateAckType.Failed);
    } else {
      ok = false;
    }
    if (event != null) {
      actorEventChannel.send(event);
    }
    responseObserver.onNext(ok ? ALLOW : REJECT);
    responseObserver.onCompleted();
  }

  @Override
  public void onGetServerMeta(GrpcServiceConfig request, StreamObserver<ServerMeta> responseObserver){
    responseObserver.onNext(this.serverMeta);
    responseObserver.onCompleted();
  }
}
