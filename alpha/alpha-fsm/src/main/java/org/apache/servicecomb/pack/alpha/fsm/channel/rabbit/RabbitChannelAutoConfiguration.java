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

package org.apache.servicecomb.pack.alpha.fsm.channel.rabbit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
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

import java.util.Map;

@EnableBinding({RabbitMessageChannel.class})
@Configuration
@EnableConfigurationProperties(BindingServiceProperties.class)
@ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "rabbit")
public class RabbitChannelAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean()
    public RabbitMessagePublisher rabbitMessagePublisher(BindingServiceProperties bindingServiceProperties, RabbitMessageChannel producerMessage) {
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();
        // 分区数量,现在现在生产者与消费这都在alpha-server，所以rabbit的分区partitionCount与该数量保持一直
        int partitionCount = bindings.get(RabbitMessageChannel.SERVICE_COMB_PACK_PRODUCER).getProducer().getPartitionCount();
        RabbitMessagePublisher messagePublisher = new RabbitMessagePublisher(partitionCount, producerMessage);
        return messagePublisher;
    }

//    @StreamMessageConverter
//    public MessageConverter StreamMessageConverter() {
//        MappingJackson2MessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2MessageConverter();
////        ObjectMapper objectMapper = new ObjectMapper();
////        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
//        return mappingJackson2HttpMessageConverter;
//    }

    @Bean
    RabbitSagaEventConsumer sagaEventRabbitConsumer(ActorSystem actorSystem,
                                                    @Qualifier("sagaShardRegionActor") ActorRef sagaShardRegionActor,
                                                    MetricsService metricsService) {
        return new RabbitSagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService);
    }

    @Bean
    @ConditionalOnMissingBean(ActorEventChannel.class)
    public ActorEventChannel kafkaEventChannel(MetricsService metricsService,
                                               @Lazy RabbitMessagePublisher rabbitMessagePublisher) {
        return new org.apache.servicecomb.pack.alpha.fsm.channel.rabbit.RabbitActorEventChannel(metricsService, rabbitMessagePublisher);
    }

}
