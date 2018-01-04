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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;
import io.netty.util.internal.ConcurrentSet;

class GrpcTxEventStreamObserver implements StreamObserver<GrpcTxEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  private final Set<SimpleImmutableEntry<String, String>> serviceEntries = new ConcurrentSet<>();

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
    String serviceName = message.getServiceName();
    String instanceId = message.getInstanceId();
    if (message.getType().equals(TxStartedEvent.name())) {
      Map<String, OmegaCallback> instanceCallback = omegaCallbacks
          .computeIfAbsent(serviceName, v -> new ConcurrentHashMap<>());
      instanceCallback.computeIfAbsent(instanceId, v -> new GrpcOmegaCallback(responseObserver));
      serviceEntries.add(new SimpleImmutableEntry<>(serviceName, instanceId));
    }

    // store received event
    txConsistentService.handle(new TxEvent(
        serviceName,
        instanceId,
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
    removeInvalidCallback();
  }

  // unless we shutdown the alpha server gracefully, this method should never be called
  @Override
  public void onCompleted() {
    LOG.info("disconnect the grpc client");
    responseObserver.onCompleted();
    removeInvalidCallback();
  }

  private void removeInvalidCallback() {
    for (SimpleImmutableEntry<String, String> entry : serviceEntries) {
      Map<String, OmegaCallback> instanceCallback = omegaCallbacks.get(entry.getKey());
      if (instanceCallback != null) {
        instanceCallback.remove(entry.getValue());
      }
    }
    serviceEntries.clear();
  }

  Set<SimpleImmutableEntry<String, String>> serviceEntries() {
    return serviceEntries;
  }
}
