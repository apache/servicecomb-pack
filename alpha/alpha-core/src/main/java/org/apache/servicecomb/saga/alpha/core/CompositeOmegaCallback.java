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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CompositeOmegaCallback implements OmegaCallback {
  private final Map<String, Map<String, OmegaCallback>> callbacks;

  public CompositeOmegaCallback(Map<String, Map<String, OmegaCallback>> callbacks) {
    this.callbacks = callbacks;
  }

  @Override
  public void retries(TxEvent event) {
    OmegaCallback omegaCallback = callbackFor(event);

    try {
      omegaCallback.retries(event);
    } catch (Exception e) {
      removeEventCallback(event, omegaCallback);
      throw e;
    }
  }

  @Override
  public void compensate(TxEvent event) {
    OmegaCallback omegaCallback = callbackFor(event);

    try {
      omegaCallback.compensate(event);
    } catch (Exception e) {
      removeEventCallback(event, omegaCallback);
      throw e;
    }
  }

  private OmegaCallback callbackFor(String instanceId, Map<String, OmegaCallback> serviceCallbacks) {
    OmegaCallback omegaCallback = serviceCallbacks.get(instanceId);
    if (Objects.isNull(omegaCallback)) {
      return serviceCallbacks.values().iterator().next();
    }
    return omegaCallback;
  }

  private OmegaCallback callbackFor(TxEvent event) {
    return Optional.ofNullable(callbacks.get(event.serviceName())).filter(callbacks -> !callbacks.isEmpty())
        .map(serviceCallbacks -> callbackFor(event.instanceId(), serviceCallbacks))
        .orElseThrow(() -> new AlphaException("No such omega callback found for service " + event.serviceName()));
  }

  private void removeEventCallback(TxEvent event, OmegaCallback omegaCallback) {
    callbacks.get(event.serviceName()).values().remove(omegaCallback);
  }
}
