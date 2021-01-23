package org.apache.servicecomb.pack.alpha.fsm.channel.rabbit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamMessageConverter;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

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
