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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryableTransport implements Transport {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Transport transport;
  private final int numberOfRetries;
  private final Fallback fallback;

  public RetryableTransport(int numberOfRetries, Transport transport, Fallback fallback) {
    if (numberOfRetries <= 0) {
      throw new IllegalArgumentException("The number of retries must be greater than 0");
    }

    this.numberOfRetries = numberOfRetries;
    this.transport = transport;
    this.fallback = fallback;
  }

  @Override
  public SagaResponse with(String address, String path, String method, Map<String, Map<String, String>> params) {
    for (int i = 0; i < numberOfRetries; i++) {
      try {
        return transport.with(address, path, method, params);
      } catch (TransactionFailedException e) {
        log.error("Failed to send {} request to {}/{} with params {}",
            method,
            address,
            path,
            params,
            e);
      }
    }

    log.warn("Falling back after {} failures sending {} request to {}/{}", numberOfRetries, method, address, path);
    return fallback.fallback();
  }
}
