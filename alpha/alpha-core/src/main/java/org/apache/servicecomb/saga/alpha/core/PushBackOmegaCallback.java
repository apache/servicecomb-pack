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
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushBackOmegaCallback implements OmegaCallback {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final BlockingQueue<Runnable> pendingCompensations;
  private final OmegaCallback underlying;

  public PushBackOmegaCallback(BlockingQueue<Runnable> pendingCompensations, OmegaCallback underlying) {
    this.pendingCompensations = pendingCompensations;
    this.underlying = underlying;
  }

  @Override
  public void retries(TxEvent event) {
    try {
      underlying.compensate(event);
    } catch (Exception e) {
      logError(event, e);
      pendingCompensations.offer(() -> compensate(event));
    }
  }

  @Override
  public void compensate(TxEvent event) {
    try {
      underlying.compensate(event);
    } catch (Exception e) {
      logError(event, e);
      pendingCompensations.offer(() -> compensate(event));
    }
  }

  private void logError(TxEvent event, Exception e) {
    log.error(
        "Failed to compensate service [{}] instance [{}] with method [{}], global tx id [{}] and local tx id [{}]",
        event.serviceName(),
        event.instanceId(),
        event.compensationMethod(),
        event.globalTxId(),
        event.localTxId(),
        e);
  }
}
