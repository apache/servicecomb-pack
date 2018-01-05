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

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfCleaningOmegaCallback implements OmegaCallback {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String instanceId;
  private final OmegaCallback underlying;
  private final Map<String, OmegaCallback> callbacks;

  SelfCleaningOmegaCallback(String instanceId, OmegaCallback underlying, Map<String, OmegaCallback> callbacks) {
    this.instanceId = instanceId;
    this.underlying = underlying;
    this.callbacks = callbacks;
  }

  @Override
  public void compensate(TxEvent event) {
    try {
      underlying.compensate(event);
    } catch (Exception e) {
      callbacks.remove(instanceId);
      log.error("Removed omega callback with instance id [{}] due to connection disruption", instanceId, e);
      throw e;
    }
  }

  @Override
  public void disconnect() {
    underlying.disconnect();
  }
}
