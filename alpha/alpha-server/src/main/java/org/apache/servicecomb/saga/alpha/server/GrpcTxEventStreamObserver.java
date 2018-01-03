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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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

class GrpcTxEventStreamObserver implements StreamObserver<GrpcTxEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Map<String, OmegaCallback>> omegaCallbacks;

  private final Map<StreamObserver<GrpcCompensateCommand>, Map<String, String>> omegaCallbacksReverse;

  private final TxConsistentService txConsistentService;

  private final StreamObserver<GrpcCompensateCommand> responseObserver;

  GrpcTxEventStreamObserver(Map<String, Map<String, OmegaCallback>> omegaCallbacks,
      Map<StreamObserver<GrpcCompensateCommand>, Map<String, String>> omegaCallbacksReverse,
      TxConsistentService txConsistentService, StreamObserver<GrpcCompensateCommand> responseObserver) {
    this.omegaCallbacks = omegaCallbacks;
    this.omegaCallbacksReverse = omegaCallbacksReverse;
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
      instanceCallback.putIfAbsent(instanceId, new GrpcOmegaCallback(responseObserver));
      Map<String, String> serviceInstanceId = omegaCallbacksReverse
          .computeIfAbsent(responseObserver, v -> new ConcurrentHashMap<>());
      serviceInstanceId.putIfAbsent(serviceName, instanceId);
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
    onCompleted();
  }

  @Override
  public void onCompleted() {
    responseObserver.onCompleted();
    removeInvalidCallback();
  }

  private void removeInvalidCallback() {
    Collection<Map<String, String>> services = omegaCallbacksReverse.values();
    for (Map<String, String> service : services) {
      Set<String> removedServices = new HashSet<>();
      for (Entry<String, String> entry : service.entrySet()) {
        String serviceName = entry.getKey();
        String instanceId = entry.getValue();
        Map<String, OmegaCallback> instanceCallback = omegaCallbacks.get(serviceName);
        if (instanceCallback != null) {
          instanceCallback.remove(instanceId);
          removedServices.add(serviceName);
        }
      }
      for (String removedService : removedServices) {
        service.remove(removedService);
      }
    }
    omegaCallbacksReverse.remove(responseObserver);
  }
}
