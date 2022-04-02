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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.AbstractActorEventChannel;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.properties.SpecSagaAkkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaActorEventChannel extends AbstractActorEventChannel {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final KafkaMessagePublisher kafkaMessagePublisher;

  public KafkaActorEventChannel(SpecSagaAkkaProperties specSagaAkkaProperties, MetricsService metricsService) {
    super(metricsService);
    // init topic
    Map props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        specSagaAkkaProperties.getChannel().getKafka().getBootstrapServers());
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 50000);
    try (final AdminClient adminClient = KafkaAdminClient.create(props)) {
      try {
        final NewTopic newTopic = new NewTopic(
            specSagaAkkaProperties.getChannel().getKafka().getTopic(),
            specSagaAkkaProperties.getChannel().getKafka().getNumPartitions(),
            specSagaAkkaProperties.getChannel().getKafka().getReplicationFactor());
        final CreateTopicsResult createTopicsResult = adminClient
            .createTopics(Collections.singleton(newTopic));
        createTopicsResult.values().get(specSagaAkkaProperties.getChannel().getKafka().getTopic())
            .get();
      } catch (InterruptedException | ExecutionException e) {
        if (e.getCause() instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        if (!(e.getCause() instanceof TopicExistsException)) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    }

    // create producer
    this.kafkaMessagePublisher = new KafkaMessagePublisher(
        specSagaAkkaProperties.getChannel().getKafka().getBootstrapServers(),
        specSagaAkkaProperties.getChannel().getKafka().getTopic(),
        specSagaAkkaProperties.getChannel().getKafka().getProducer());
    LOG.info("Kafka Channel Init");
  }

  @Override
  public void sendTo(BaseEvent event) {
    kafkaMessagePublisher.publish(event);
  }
}
