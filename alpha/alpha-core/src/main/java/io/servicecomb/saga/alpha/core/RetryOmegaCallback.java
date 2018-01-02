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

package io.servicecomb.saga.alpha.core;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryOmegaCallback implements OmegaCallback {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String ERROR_MESSAGE = "Failed to compensate service [{}] instance [{}] with method [{}], global tx id [{}] and local tx id [{}]";

  private final OmegaCallback underlying;
  private final int delay;

  public RetryOmegaCallback(OmegaCallback underlying, int delay) {
    this.underlying = underlying;
    this.delay = delay;
  }

  @Override
  public void compensate(TxEvent event) {
    boolean success = false;
    do {
      try {
        underlying.compensate(event);
        success = true;
      } catch (Exception e) {
        logError(ERROR_MESSAGE, event, e);
        sleep(event);
      }
    } while (!success && !Thread.currentThread().isInterrupted());
  }

  private void sleep(TxEvent event) {
    try {
      TimeUnit.MILLISECONDS.sleep(delay);
    } catch (InterruptedException e) {
      logError(ERROR_MESSAGE + " due to interruption", event, e);

      Thread.currentThread().interrupt();
    }
  }

  private void logError(String message, TxEvent event, Exception e) {
    log.error(message,
        event.serviceName(),
        event.instanceId(),
        event.compensationMethod(),
        event.globalTxId(),
        event.localTxId(),
        e);
  }
}
