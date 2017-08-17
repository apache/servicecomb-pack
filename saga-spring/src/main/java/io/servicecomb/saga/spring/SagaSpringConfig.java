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

import io.servicecomb.saga.core.EventEnvelope;
import io.servicecomb.saga.core.PersistentStore;
import io.servicecomb.saga.core.Transport;
import io.servicecomb.saga.infrastructure.EmbeddedEventStore;
import io.servicecomb.saga.transports.httpclient.HttpClientTransport;
import java.util.Collections;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SagaSpringConfig {

  @Bean
  Transport transport() {
    return new HttpClientTransport();
  }

  @Bean
  PersistentStore persistentStore() {
    return new EmbeddedPersistentStore();
  }

  private static class EmbeddedPersistentStore extends EmbeddedEventStore implements PersistentStore {

    @Override
    public Map<Long, Iterable<EventEnvelope>> findPendingSagaEvents() {
      return Collections.emptyMap();
    }
  }
}
