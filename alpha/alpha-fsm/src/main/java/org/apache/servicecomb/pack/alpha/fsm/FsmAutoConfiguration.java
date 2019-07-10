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

package org.apache.servicecomb.pack.alpha.fsm;

import static org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SpringAkkaExtension.SPRING_EXTENSION_PROVIDER;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.fsm.event.consumer.SagaEventActorEventSender;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.AkkaConfigPropertyAdapter;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.eventbus.EventSubscribeBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
@ConditionalOnProperty(value = {"alpha.feature.akka.enabled"})
public class FsmAutoConfiguration {

  @Bean
  public ActorSystem actorSystem(ConfigurableApplicationContext applicationContext, ConfigurableEnvironment environment) {
    ActorSystem system = ActorSystem.create("alpha-akka", akkaConfiguration(applicationContext,environment));
    SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
    return system;
  }

  @Bean
  public Config akkaConfiguration(ConfigurableApplicationContext applicationContext, ConfigurableEnvironment environment) {
    final Map<String, Object> converted = AkkaConfigPropertyAdapter.getPropertyMap(environment);
    return ConfigFactory.parseMap(converted).withFallback(ConfigFactory.defaultReference(applicationContext.getClassLoader()));
  }

  @Bean
  public SagaEventActorEventSender sagaEventConsumer(){
    return new SagaEventActorEventSender();
  }

  @Bean
  public EventSubscribeBeanPostProcessor eventSubscribeBeanPostProcessor(){
    return new EventSubscribeBeanPostProcessor();
  }

}
