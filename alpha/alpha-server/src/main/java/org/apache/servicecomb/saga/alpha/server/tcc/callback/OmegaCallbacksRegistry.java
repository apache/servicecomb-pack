/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server.tcc.callback;

import static java.util.Collections.emptyMap;

import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.saga.alpha.core.AlphaException;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.GrpcOmegaTccCallback;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.OmegaCallback;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage omega callbacks.
 */
public final class OmegaCallbacksRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final static Map<String, Map<String, OmegaCallback>> REGISTRY = new ConcurrentHashMap<>();

  public static Map<String, Map<String, OmegaCallback>> getRegistry() {
    return REGISTRY;
  }

  /**
   * Register omega TCC callback.
   *
   * @param request Grpc service config
   * @param responseObserver stream observer
   */
  public static void register(GrpcServiceConfig request, StreamObserver<GrpcTccCoordinateCommand> responseObserver) {
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
   * @throws AlphaException trigger this exception while missing omega callback by service name
   */
  public static OmegaCallback retrieve(String serviceName, String instanceId) throws AlphaException {
    Map<String, OmegaCallback> callbackMap = REGISTRY.getOrDefault(serviceName, emptyMap());
    if (callbackMap.isEmpty()) {
      throw new AlphaException("No such omega callback found for service " + serviceName);
    }
    OmegaCallback result = callbackMap.get(instanceId);
    if (null == result) {
      LOG.info("Cannot find the service with the instanceId {}, call the other instance.", instanceId);
      return callbackMap.values().iterator().next();
    }
    return result;
  }

  /**
   * Remove omega TCC callback by service name and instance id.
   *
   * @param serviceName service name
   * @param instanceId instance id
   */
  public static void remove(String serviceName, String instanceId) {
    REGISTRY.getOrDefault(serviceName, emptyMap()).remove(instanceId);
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
