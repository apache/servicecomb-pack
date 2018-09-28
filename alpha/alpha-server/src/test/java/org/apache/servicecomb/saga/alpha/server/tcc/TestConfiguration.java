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

package org.apache.servicecomb.saga.alpha.server.tcc;

import org.apache.servicecomb.saga.alpha.server.GrpcServerConfig;
import org.apache.servicecomb.saga.alpha.server.GrpcStartable;
import org.apache.servicecomb.saga.alpha.server.ServerStartable;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.TccPendingTaskRunner;
import org.apache.servicecomb.saga.alpha.server.tcc.service.TccTxEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Bean
  TccPendingTaskRunner tccPendingTaskRunner() {
    return new TccPendingTaskRunner(delay);
  }

  @Bean
  GrpcTccEventService grpcTccEventService(TccTxEventService tccTxEventService, TccPendingTaskRunner tccPendingTaskRunner) {
    tccPendingTaskRunner.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> tccPendingTaskRunner.shutdown()));
    return new GrpcTccEventService(tccTxEventService);
  }

  @Bean
  ServerStartable serverStartable(GrpcServerConfig serverConfig, GrpcTccEventService grpcTccEventService) {
    ServerStartable bootstrap = new GrpcStartable(serverConfig, grpcTccEventService);
    new Thread(bootstrap::start).start();
    return bootstrap;
  }

}
