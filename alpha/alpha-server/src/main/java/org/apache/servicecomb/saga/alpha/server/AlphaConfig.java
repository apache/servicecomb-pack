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

package org.apache.servicecomb.saga.alpha.server;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.apache.servicecomb.saga.alpha.core.CompositeOmegaCallback;
import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.PendingTaskRunner;
import org.apache.servicecomb.saga.alpha.core.PushBackOmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AlphaConfig {
  private final BlockingQueue<Runnable> pendingCompensations = new LinkedBlockingQueue<>();

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Bean
  Map<String, Map<String, OmegaCallback>> omegaCallbacks() {
    return new ConcurrentHashMap<>();
  }

  @Bean
  OmegaCallback omegaCallback(Map<String, Map<String, OmegaCallback>> callbacks) {
    return new PushBackOmegaCallback(pendingCompensations, new CompositeOmegaCallback(callbacks));
  }
  
  @Bean
  TxEventRepository springTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    return new SpringTxEventRepository(eventRepo);
  }

  @Bean
  TxConsistentService txConsistentService(@Value("${alpha.server.port:8080}") int port,
      TxEventRepository eventRepository,
      OmegaCallback omegaCallback,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks) {

    TxConsistentService consistentService = new TxConsistentService(eventRepository, omegaCallback);

    ServerStartable startable = buildGrpc(port, consistentService, omegaCallbacks);
    new Thread(startable::start).start();

    return consistentService;
  }

  private ServerStartable buildGrpc(int port, TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks) {
    return new GrpcStartable(port,
        new GrpcTxEventEndpointImpl(txConsistentService, omegaCallbacks));
  }

  @PostConstruct
  void init() {
    new PendingTaskRunner(pendingCompensations, delay).run();
  }
}
