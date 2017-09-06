/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.core;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryableCompensation implements Compensation {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final int retries;
  private final int retryDelay;
  private final Compensation compensation;
  private final Fallback fallback;

  public RetryableCompensation(int retries, int retryDelay, Compensation compensation, Fallback fallback) {
    if (retries <= 0) {
      throw new IllegalArgumentException("The number of retries must be greater than 0");
    }

    this.retries = retries;
    this.retryDelay = retryDelay;
    this.compensation = compensation;
    this.fallback = fallback;
  }

  @Override
  public SagaResponse send(String address) {
    for (int i = 0; i < retries && !Thread.currentThread().isInterrupted(); i++) {
      try {
        return compensation.send(address);
      } catch (TransactionFailedException e) {
        log.error("Failed to send compensation to {}", address, e);
        sleep(retryDelay);
      }
    }

    log.warn("Falling back after {} failures sending compensation to {}", retries, address);
    return fallback.fallback();
  }

  private void sleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}
