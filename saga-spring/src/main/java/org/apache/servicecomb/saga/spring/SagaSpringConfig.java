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

package org.apache.servicecomb.saga.spring;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.servicecomb.saga.core.JacksonToJsonFormat;
import org.apache.servicecomb.saga.core.ToJsonFormat;
import org.apache.servicecomb.saga.core.actors.ActorBasedSagaFactory;
import org.apache.servicecomb.saga.core.application.SagaExecutionComponent;
import org.apache.servicecomb.saga.core.application.SagaFactory;
import org.apache.servicecomb.saga.core.dag.GraphBasedSagaFactory;
import org.apache.servicecomb.saga.format.ChildrenExtractor;
import org.apache.servicecomb.saga.format.JacksonFromJsonFormat;
import org.apache.servicecomb.saga.format.SagaEventFormat;
import org.apache.servicecomb.saga.transports.TransportFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.servicecomb.saga.core.PersistentStore;
import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;
import org.apache.servicecomb.saga.format.JacksonSagaEventFormat;
import org.apache.servicecomb.saga.transports.RestTransport;

@Configuration
class SagaSpringConfig {

  @Bean
  TransportFactory transportFactory(RestTransport restTransport) {
    return () -> restTransport;
  }

  @Bean
  FromJsonFormat<SagaDefinition> fromJsonFormat(TransportFactory transportFactory) {
    return new JacksonFromJsonFormat(transportFactory);
  }

  @Bean
  ToJsonFormat toJsonFormat() {
    return new JacksonToJsonFormat();
  }

  @Bean
  SagaEventFormat sagaEventFormat(TransportFactory transportFactory) {
    return new JacksonSagaEventFormat(transportFactory);
  }

  @Bean
  PersistentStore persistentStore(SagaEventRepo repo, ToJsonFormat toJsonFormat, SagaEventFormat eventFormat) {
    return new JpaPersistentStore(repo, toJsonFormat, eventFormat);
  }

  @Bean
  SagaExecutionQueryService queryService(SagaEventRepo repo, FromJsonFormat<SagaDefinition> fromJsonFormat) {
    return new SagaExecutionQueryService(repo, fromJsonFormat);
  }

  @Bean
  SagaExecutionComponent sagaExecutionComponent(
      PersistentStore persistentStore,
      ToJsonFormat format,
      FromJsonFormat<SagaDefinition> fromJsonFormat,
      SagaFactory sagaFactory) {

    return new SagaExecutionComponent(
        persistentStore,
        fromJsonFormat,
        format,
        sagaFactory);
  }

  @Bean
  FromJsonFormat<Set<String>> childrenExtractor() {
    return new ChildrenExtractor();
  }

  @ConditionalOnProperty(value = "saga.runningMode", havingValue = "graph", matchIfMissing = true)
  @Bean
  SagaFactory graphBasedSagaFactory(
      @Value("${saga.thread.count:5}") int numberOfThreads,
      @Value("${saga.retry.delay:3000}") int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat<Set<String>> childrenExtractor) {

    return new GraphBasedSagaFactory(
        retryDelay,
        persistentStore,
        childrenExtractor,
        Executors.newFixedThreadPool(numberOfThreads, sagaThreadFactory()));
  }

  @ConditionalOnProperty(value = "saga.runningMode", havingValue = "actor")
  @Bean
  SagaFactory actorBasedSagaFactory(
      @Value("${saga.retry.delay:3000}") int retryDelay,
      PersistentStore persistentStore,
      FromJsonFormat<Set<String>> childrenExtractor) {

    return new ActorBasedSagaFactory(
        retryDelay,
        persistentStore,
        childrenExtractor);
  }

  private ThreadFactory sagaThreadFactory() {
    return new ThreadFactory() {
      private final AtomicInteger threadCount = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "saga-pool-thread-" + threadCount.incrementAndGet());
      }
    };
  }
}
