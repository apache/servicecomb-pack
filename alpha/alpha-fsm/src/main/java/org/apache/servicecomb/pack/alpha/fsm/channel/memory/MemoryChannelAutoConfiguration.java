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
package org.apache.servicecomb.pack.alpha.fsm.channel.memory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.lang.invoke.MethodHandles;
import javax.annotation.PostConstruct;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "memory", matchIfMissing = true)
public class MemoryChannelAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${alpha.feature.akka.channel.memory.size:-1}")
  int memoryEventChannelMemorySize;

  @PostConstruct
  public void init(){
    LOG.info("Memory Channel Init");
  }

  @Bean(name = "memoryEventChannel")
  @ConditionalOnMissingBean(ActorEventChannel.class)
  public ActorEventChannel memoryEventChannel(MetricsService metricsService) {
    return new MemoryActorEventChannel(metricsService, memoryEventChannelMemorySize);
  }

  @Bean
  MemorySagaEventConsumer sagaEventMemoryConsumer(ActorSystem actorSystem,
      @Qualifier("sagaShardRegionActor") ActorRef sagaShardRegionActor,
      MetricsService metricsService,
      @Qualifier("memoryEventChannel") ActorEventChannel actorEventChannel) {
    return new MemorySagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService,
        (MemoryActorEventChannel) actorEventChannel);
  }
}