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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.kafka;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.MessagePublisher;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class KafkaMessagePublisher implements MessagePublisher<BaseEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessagePublisher.class);

    private String topic;
    private KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMessagePublisher(String bootstrap_servers, String topic, Map<String,String> producerConfigMap) {
        this.topic = topic;
        Map<String, Object> map = Maps.newHashMap();
        producerConfigMap.forEach((k, v) -> map.put(k, v));
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrap_servers);
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        this.kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(map));
    }

    @Override
    public void publish(BaseEvent data) {
        if(LOG.isDebugEnabled()){
            LOG.debug("send [{}] {} {}", data.getGlobalTxId(), data.getType(), data.getLocalTxId());
        }
        try {
            kafkaTemplate.send(topic, data.getGlobalTxId(), data).get();
        } catch (InterruptedException | ExecutionException | UnsupportedOperationException e) {
            if (e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }
}
