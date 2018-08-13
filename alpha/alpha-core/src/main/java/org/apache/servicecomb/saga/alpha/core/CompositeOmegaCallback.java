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

package org.apache.servicecomb.saga.alpha.core;

import static java.util.Collections.emptyMap;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeOmegaCallback implements OmegaCallback {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<String, Map<String, OmegaCallback>> callbacks;

  public CompositeOmegaCallback(Map<String, Map<String, OmegaCallback>> callbacks) {
    this.callbacks = callbacks;
  }

  @Override
  public void compensate(TxEvent event) {
    Map<String, OmegaCallback> serviceCallbacks = callbacks.getOrDefault(event.serviceName(), emptyMap());

    if (serviceCallbacks.isEmpty()) {
      throw new AlphaException("No such omega callback found for service " + event.serviceName());
    }

    OmegaCallback omegaCallback = serviceCallbacks.get(event.instanceId());
    if (omegaCallback == null) {
      LOG.info("Cannot find the service with the instanceId {}, call the other instance.", event.instanceId());
      omegaCallback = serviceCallbacks.values().iterator().next();
    }

    try {
      omegaCallback.compensate(event);
    } catch (Exception e) {
      serviceCallbacks.values().remove(omegaCallback);
      throw e;
    }
  }
}
