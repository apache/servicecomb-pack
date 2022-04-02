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

package org.apache.servicecomb.pack.alpha.spec.tcc.db;

import com.google.common.eventbus.EventBus;
import org.apache.servicecomb.pack.alpha.server.GrpcServerConfig;
import org.apache.servicecomb.pack.alpha.server.GrpcStartable;
import org.apache.servicecomb.pack.alpha.server.ServerStartable;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.callback.TccPendingTaskRunner;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccEventScanner;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccTxEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/**
 * This configuration is just for speeding up the integration usage.
 */
@Configuration
@Profile("tccTest")
public class TccConfiguration {

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Value("${alpha.tx.timeout-seconds:600}")
  private int globalTxTimeoutSeconds;

  @Bean
  EventBus alphaEventBus() {
    return new EventBus("alphaEventBus");
  }

  @Bean
  TccPendingTaskRunner tccPendingTaskRunner() {
    return new TccPendingTaskRunner(delay);
  }

  @Bean
  GrpcTccEventService grpcTccEventService(TccTxEventService tccTxEventService) {
    return new GrpcTccEventService(tccTxEventService);
  }

  @Bean
  TccEventScanner tccEventScanner(TccTxEventService tccTxEventService) {
    return new TccEventScanner(tccTxEventService, delay, globalTxTimeoutSeconds);
  }

  @Bean
  ServerStartable serverStartable(GrpcServerConfig serverConfig, GrpcTccEventService grpcTccEventService,
      TccPendingTaskRunner tccPendingTaskRunner, TccEventScanner tccEventScanner, EventBus eventBus) throws IOException {
    ServerStartable bootstrap = new GrpcStartable(serverConfig, eventBus, grpcTccEventService);
    new Thread(bootstrap::start).start();

    tccPendingTaskRunner.start();
    tccEventScanner.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      tccPendingTaskRunner.shutdown();
      tccEventScanner.shutdown();
    }));

    return bootstrap;
  }
}
