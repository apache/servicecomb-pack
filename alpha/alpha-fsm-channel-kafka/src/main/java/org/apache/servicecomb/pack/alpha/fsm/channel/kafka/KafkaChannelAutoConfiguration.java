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
package org.apache.servicecomb.pack.alpha.fsm.channel.kafka;

import com.google.common.collect.Maps;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.servicecomb.pack.alpha.core.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
@ConditionalOnClass(KafkaProperties.class)
@ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "kafka")
public class KafkaChannelAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(KafkaChannelAutoConfiguration.class);

    @Value("${alpha.feature.akka.channel.kafka.topic:servicecomb-pack-actor-event}")
    private String topic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap_servers;

    @Value("${spring.kafka.consumer.group-id:servicecomb-pack}")
    private String groupId;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:org.apache.servicecomb.pack.alpha.core.fsm.event,org.apache.servicecomb.pack.alpha.core.fsm.event.base,}org.apache.servicecomb.pack.alpha.core.fsm.event.internal")
    private String trusted_packages;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private int batchSize;

    @Value("${spring.kafka.producer.retries:0}")
    private int retries;

    @Value("${spring.kafka.producer.buffer.memory:33364432}")
    private long bufferMemory;


    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> producerFactory(){
        Map<String, Object> map = Maps.newHashMap();
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        map.put(ProducerConfig.RETRIES_CONFIG, retries);
        map.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        map.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);

        return new DefaultKafkaProducerFactory<>(map);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> consumerFactory(){
        Map<String, Object> map = Maps.newHashMap();

        map.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        map.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        map.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        map.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        map.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        map.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        map.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 100);
        map.put(JsonDeserializer.TRUSTED_PACKAGES, trusted_packages);

        if(logger.isDebugEnabled()){
            logger.debug("init consumerFactory properties = [{}]", map);
        }
        return new DefaultKafkaConsumerFactory<>(map);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String,Object> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory());
        concurrentKafkaListenerContainerFactory.getContainerProperties().setPollTimeout(1500L);

        return concurrentKafkaListenerContainerFactory;
    }
    @Bean
    @ConditionalOnMissingBean
    public KafkaMessagePublisher kafkaMessagePublisher(KafkaTemplate<String, Object> kafkaTemplate){
        return new KafkaMessagePublisher(topic, kafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaMessageListener kafkaMessageListener(@Lazy @Qualifier("actorEventSink") ActorEventSink actorEventSink){
        return new KafkaMessageListener(actorEventSink);
    }
}