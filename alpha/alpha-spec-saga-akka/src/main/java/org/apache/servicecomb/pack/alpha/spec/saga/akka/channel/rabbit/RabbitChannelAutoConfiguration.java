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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.rabbit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@EnableBinding({RabbitMessageChannel.class})
@Configuration
@EnableConfigurationProperties(BindingServiceProperties.class)
@ConditionalOnProperty(value = "alpha.spec.saga.akka.channel.name", havingValue = "rabbit")
public class RabbitChannelAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean()
    public RabbitMessagePublisher rabbitMessagePublisher(BindingServiceProperties bindingServiceProperties, RabbitMessageChannel producerMessage) {
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();
        // partitionCount must consistent with alpha server because of alpha server contains the consumer
        int partitionCount = bindings.get(RabbitMessageChannel.SERVICE_COMB_PACK_PRODUCER).getProducer().getPartitionCount();
        RabbitMessagePublisher messagePublisher = new RabbitMessagePublisher(partitionCount, producerMessage);
        return messagePublisher;
    }

    @Bean
    RabbitSagaEventConsumer sagaEventRabbitConsumer(ActorSystem actorSystem,
                                                    @Qualifier("sagaShardRegionActor") ActorRef sagaShardRegionActor,
                                                    MetricsService metricsService) {
        return new RabbitSagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService);
    }

    @Bean
    @ConditionalOnMissingBean(ActorEventChannel.class)
    public ActorEventChannel rabbitEventChannel(MetricsService metricsService,
                                               @Lazy RabbitMessagePublisher rabbitMessagePublisher) {
        return new RabbitActorEventChannel(metricsService, rabbitMessagePublisher);
    }

}
