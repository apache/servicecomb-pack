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

import static java.util.Collections.emptyMap;

import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.saga.alpha.tcc.server.event.ParticipateEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCordinateCommand;

/**
 * Manage Omega callbacks.
 *
 * @author zhaojun
 */
public class OmegaCallbacksRegistry {

  private final static Map<String, Map<String, OmegaCallback>> CALLBACKS = new ConcurrentHashMap<>();

  public static void add(GrpcServiceConfig request, StreamObserver<GrpcTccCordinateCommand> responseObserver) {
    CALLBACKS
        .computeIfAbsent(request.getServiceName(), key -> new ConcurrentHashMap<>())
        .put(request.getInstanceId(), new GrpcOmegaTccCallback(responseObserver));
  }

  public static OmegaCallback getThenRemove(ParticipateEvent request) {
    return CALLBACKS.getOrDefault(request.getServiceName(), emptyMap()).remove(request.getInstanceId());
  }

}
