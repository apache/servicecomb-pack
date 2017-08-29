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

package io.servicecomb.saga.spring;

import io.servicecomb.saga.core.JacksonToJsonFormat;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.ToJsonFormat;
import io.servicecomb.saga.core.Transport;
import io.servicecomb.saga.core.application.SagaCoordinator;
import io.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import io.servicecomb.saga.format.JacksonFromJsonFormat;
import io.servicecomb.saga.core.application.interpreter.JsonRequestInterpreter;
import io.servicecomb.saga.format.JacksonSagaEventFormat;
import io.servicecomb.saga.format.SagaEventFormat;
import io.servicecomb.saga.transports.httpclient.HttpClientTransport;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SagaSpringConfig {

  @Bean
  FromJsonFormat fromJsonFormat() {
    return new JacksonFromJsonFormat();
  }

  @Bean
  ToJsonFormat toJsonFormat() {
    return new JacksonToJsonFormat();
  }

  @Bean
  SagaEventFormat sagaEventFormat() {
    return new JacksonSagaEventFormat();
  }

  @Bean
  Transport transport() {
    return new HttpClientTransport();
  }

  @Bean
  PersistentStore persistentStore(SagaEventRepo repo, ToJsonFormat toJsonFormat, SagaEventFormat eventFormat) {
    return new JpaPersistentStore(repo, toJsonFormat, eventFormat);
  }

  @Bean
  SagaCoordinator sagaCoordinator(
      @Value("${saga.thread.count:5}") int numberOfThreads,
      PersistentStore persistentStore,
      Transport transport,
      ToJsonFormat format,
      FromJsonFormat fromJsonFormat) {

    return new SagaCoordinator(
        persistentStore,
        new JsonRequestInterpreter(fromJsonFormat),
        format,
        transport,
        Executors.newFixedThreadPool(numberOfThreads));
  }
}
