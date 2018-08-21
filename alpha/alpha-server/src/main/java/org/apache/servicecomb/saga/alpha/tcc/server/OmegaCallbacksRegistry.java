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
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCordinateCommand;

/**
 * Manage Omega callbacks.
 *
 * @author zhaojun
 */
public final class OmegaCallbacksRegistry {

  private final static Map<String, Map<String, OmegaCallback>> REGISTRY = new ConcurrentHashMap<>();

  /**
   * Register omega TCC callback.
   *
   * @param request Grpc service config
   * @param responseObserver stream observer
   */
  public static void register(GrpcServiceConfig request, StreamObserver<GrpcTccCordinateCommand> responseObserver) {
    REGISTRY
        .computeIfAbsent(request.getServiceName(), key -> new ConcurrentHashMap<>())
        .put(request.getInstanceId(), new GrpcOmegaTccCallback(responseObserver));
  }

  /**
   * Retrieve omega TCC callback by service name and instance id.
   *
   * @param serviceName service name
   * @param instanceId instance id
   * @return Grpc omega TCC callback
   */
  public static OmegaCallback retrieve(String serviceName, String instanceId) {
    return REGISTRY.getOrDefault(serviceName, emptyMap()).get(instanceId);
  }

  /**
   * Retrieve omega TCC callback by service name and instance id, then remove it from registry.
   *
   * @param serviceName service name
   * @param instanceId instance id
   * @return Grpc omega TCC callback
   */
  public static OmegaCallback retrieveThenRemove(String serviceName, String instanceId) {
    return REGISTRY.getOrDefault(serviceName, emptyMap()).remove(instanceId);
  }

}
