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
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.AbstractEventConsumer;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class KafkaSagaEventConsumer extends AbstractEventConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  final String groupId = "servicecomb-pack";
  final ObjectMapper jsonMapper = new ObjectMapper();

  public KafkaSagaEventConsumer(ActorSystem actorSystem, ActorRef sagaShardRegionActor,
      MetricsService metricsService, String bootstrap_servers, String topic, Map<String,String> consumerConfigMap) {
    super(actorSystem, sagaShardRegionActor, metricsService);


    // init consumer
    final Materializer materializer = ActorMaterializer.create(actorSystem);
    final Config consumerConfig = actorSystem.settings().config().getConfig("akka.kafka.consumer");
    final ConsumerSettings<String, String> consumerSettings =
        ConsumerSettings
            .create(consumerConfig, new StringDeserializer(), new StringDeserializer())
            .withBootstrapServers(bootstrap_servers)
            .withGroupId(groupId)
            .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
            .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .withProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "StringDeserializer.class")
            .withProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "StringDeserializer.class");
    consumerConfigMap.forEach((k,v) -> consumerSettings.withProperty(k,v));
    Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
        .mapAsync(20, event -> {
          BaseEvent bean = jsonMapper.readValue(event.record().value(), BaseEvent.class);
          if (LOG.isDebugEnabled()) {
            LOG.debug("receive [{}] {} {}", bean.getGlobalTxId(), bean.getType(), bean.getLocalTxId());
          }
          return sendSagaActor(bean).thenApply(done -> event.committableOffset());
        })
        .batch(
            100,
            ConsumerMessage::createCommittableOffsetBatch,
            ConsumerMessage.CommittableOffsetBatch::updated
        )
        .mapAsync(20, offset -> offset.commitJavadsl())
        .to(Sink.ignore())
        .run(materializer);
  }

  private CompletionStage<String> sendSagaActor(BaseEvent event) {
    try {
      long begin = System.currentTimeMillis();
      metricsService.metrics().doActorReceived();
      // Use the synchronous method call to ensure that Kafka's Offset is set after the delivery is successful.
      Timeout timeout = new Timeout(Duration.create(10, "seconds"));
      Future<Object> future = Patterns.ask(sagaShardRegionActor, event, timeout);
      Await.result(future, timeout.duration());
      long end = System.currentTimeMillis();
      metricsService.metrics().doActorAccepted();
      metricsService.metrics().doActorAvgTime(end - begin);
      return CompletableFuture.completedFuture("OK");
    } catch (Exception ex) {
      LOG.error(ex.getMessage(),ex);
      metricsService.metrics().doActorRejected();
      throw new CompletionException(ex);
    }
  }
}
