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

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.servicecomb.saga.alpha.core.TxEventRepository;

@Configuration
class AlphaConfig {

  @Bean
  TxEventRepository springTxEventRepository(@Value("${alpha.server.port:8080}") int port, TxEventEnvelopeRepository eventRepo) {
    TxEventRepository eventRepository = new SpringTxEventRepository(eventRepo);

    ThriftStartable startable = new ThriftStartable(port, new SwiftTxEventEndpointImpl(eventRepository));
    CompletableFuture.runAsync(startable::start);

    return eventRepository;
  }
}
