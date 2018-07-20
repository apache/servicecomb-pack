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

package org.apache.servicecomb.saga.core;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallbackPolicy {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final int retryDelay;

  public FallbackPolicy(int retryDelay) {
    this.retryDelay = retryDelay;
  }

  public SagaResponse apply(String address, Compensation compensation, Fallback fallback) {
    for (int i = 0; isRetryable(i, compensation) && !isInterrupted(); i++) {
      try {
        return compensation.send(address);
      } catch (Exception e) {
        log.error("Failed to send compensation to {}", address, e);
        sleep(retryDelay);
      }
    }

    log.warn("Falling back after {} failures sending compensation to {}", compensation.retries(), address);
    return fallback.send(address);
  }

  private boolean isRetryable(int i, Compensation compensation) {
    return compensation.retries() < 0 || i <= compensation.retries();
  }

  private boolean isInterrupted() {
    return Thread.currentThread().isInterrupted();
  }

  private void sleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}
