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

package io.servicecomb.saga.alpha.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.servicecomb.saga.alpha.core.CompositeOmegaCallback;
import io.servicecomb.saga.alpha.core.OmegaCallback;
import io.servicecomb.saga.alpha.core.RetryOmegaCallback;
import io.servicecomb.saga.alpha.core.TxConsistentService;
import io.servicecomb.saga.alpha.core.TxEventRepository;

@Configuration
class AlphaConfig {

  // TODO: 2017/12/27 to be filled with actual callbacks on completion of SCB-138
  @Bean
  Map<String, Map<String, OmegaCallback>> omegaCallbacks() {
    return new ConcurrentHashMap<>();
  }

  @Bean
  OmegaCallback omegaCallback(
      Map<String, Map<String, OmegaCallback>> callbacks,
      @Value("${alpha.compensation.retry.delay:3000}") int delay) {

    return new RetryOmegaCallback(new CompositeOmegaCallback(callbacks), delay);
  }
  
  @Bean
  TxEventRepository springTxEventRepository(@Value("${alpha.server.port:8080}") int port,
      TxEventEnvelopeRepository eventRepo,
      OmegaCallback omegaCallback) {

    TxEventRepository eventRepository = new SpringTxEventRepository(eventRepo);

    ServerStartable startable = buildGrpc(port, omegaCallback, eventRepository);
    new Thread(startable::start).start();

    return eventRepository;
  }

  private ServerStartable buildGrpc(int port, OmegaCallback omegaCallback, TxEventRepository eventRepository) {
    return new GrpcStartable(
        port,
        new GrpcTxEventEndpointImpl(
            new TxConsistentService(
                eventRepository,
                omegaCallback)));
  }
}
