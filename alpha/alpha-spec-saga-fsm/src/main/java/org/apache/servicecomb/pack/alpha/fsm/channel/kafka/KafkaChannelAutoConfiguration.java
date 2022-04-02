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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.collect.Maps;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
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
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@ConditionalOnClass(KafkaProperties.class)
@ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "kafka")
public class KafkaChannelAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  @Value("${spring.kafka.producer.buffer.memory:33554432}")
  private long bufferMemory;

  @Value("${spring.kafka.consumer.auto.offset.reset:earliest}")
  private String autoOffsetReset;

  @Value("${spring.kafka.consumer.enable.auto.commit:false}")
  private boolean enableAutoCommit;

  @Value("${spring.kafka.consumer.auto.commit.interval.ms:100}")
  private int autoCommitIntervalMs;

  @Value("${spring.kafka.listener.ackMode:MANUAL_IMMEDIATE}")
  private String ackMode;

  @Value("${spring.kafka.listener.pollTimeout:1500}")
  private long poolTimeout;

  @Value("${kafka.numPartitions:6}")
  private int numPartitions;

  @Value("${kafka.replicationFactor:1}")
  private short replicationFactor;

  @PostConstruct
  public void init() {
    Map props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 50000);
    try (final AdminClient adminClient = KafkaAdminClient.create(props)) {
      try {
        final NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor);
        final CreateTopicsResult createTopicsResult = adminClient
            .createTopics(Collections.singleton(newTopic));
        createTopicsResult.values().get(topic).get();
      } catch (InterruptedException | ExecutionException e) {
        if (e.getCause() instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        if (!(e.getCause() instanceof TopicExistsException)) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    }
    LOG.info("Kafka Channel Init");
  }

  @Bean
  @ConditionalOnMissingBean
  public KafkaMessagePublisher kafkaMessagePublisher() {
    Map<String, Object> map = Maps.newHashMap();
    map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
    map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    map.put(ProducerConfig.RETRIES_CONFIG, retries);
    map.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
    map.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
    return new KafkaMessagePublisher(topic,
        new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(map)));
  }

  @Bean
  @ConditionalOnMissingBean(ActorEventChannel.class)
  public ActorEventChannel kafkaEventChannel(MetricsService metricsService,
      @Lazy KafkaMessagePublisher kafkaMessagePublisher) {
    return new KafkaActorEventChannel(metricsService, kafkaMessagePublisher);
  }

  @Bean
  KafkaSagaEventConsumer sagaEventKafkaConsumer(ActorSystem actorSystem,
      @Qualifier("sagaShardRegionActor") ActorRef sagaShardRegionActor,
      MetricsService metricsService) {
    return new KafkaSagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService,
        bootstrap_servers, topic);
  }
}
