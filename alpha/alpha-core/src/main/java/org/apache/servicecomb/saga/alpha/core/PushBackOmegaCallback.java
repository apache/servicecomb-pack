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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyMap;

public class PushBackOmegaCallback implements OmegaCallback {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, Map<String, OmegaCallback>> callbacks;
  private final ExecutorService compensateExecutor;

  public PushBackOmegaCallback(Map<String, Map<String, OmegaCallback>> callbacks,
      ExecutorService compensateExecutor) {
    this.callbacks = callbacks;
    this.compensateExecutor = compensateExecutor;
  }

  @Override
  public List<TxEvent> compensateAllEvents(List<TxEvent> txEvents) {
    List<Future<List<TxEvent>>> futures = new ArrayList<>();
    List<TxEvent> result = new ArrayList<>();
    Set<String> services = new HashSet<>();
    txEvents.stream()
        .filter(txEvent -> !callbacks.getOrDefault(txEvent.serviceName(), emptyMap()).isEmpty())
        .forEach(event -> services.add(event.serviceName()));
    services.forEach(service -> futures.add(compensateExecutor.submit(
        new CompositeOmegaCallbackRunner(callbacks,
            txEvents.stream().filter(txEvent -> txEvent.serviceName().equals(service))
                .collect(Collectors.toList())))));
    futures.forEach(f -> {
      try {
        result.addAll(f.get());
      } catch (Exception e) {
        LOG.error("Run compensate thread failed. Error message is {}.", e);
      }
    });
    return result;
  }

  @Override
  public void compensate(TxEvent event) {

  }


}
