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
package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.redis;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.lang.invoke.MethodHandles;
import javax.annotation.PostConstruct;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.MessagePublisher;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnClass(RedisConnection.class)
@ConditionalOnProperty(value = "alpha.spec.saga.akka.channel.name", havingValue = "redis")
public class RedisChannelAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${alpha.feature.akka.channel.redis.topic:servicecomb-pack-actor-event}")
  private String topic;

  @PostConstruct
  public void init() {
    LOG.info("Redis Channel Init");
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new GenericToStringSerializer<>(Object.class));
    redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
    redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
    redisTemplate.setConnectionFactory(redisConnectionFactory);

    return redisTemplate;
  }

  @Bean
  RedisSagaEventConsumer redisSagaEventConsumer(ActorSystem actorSystem,
      @Qualifier("sagaShardRegionActor") ActorRef sagaShardRegionActor,
      MetricsService metricsService,
      @Lazy @Qualifier("nodeStatus") NodeStatus nodeStatus) {
    return new RedisSagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService,
        nodeStatus);
  }

  @Bean
  public MessageListenerAdapter messageListenerAdapter(
      RedisSagaEventConsumer redisSagaEventConsumer) {
    return new MessageListenerAdapter(redisSagaEventConsumer);
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory redisConnectionFactory,
      RedisSagaEventConsumer redisSagaEventConsumer) {
    RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();

    redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
    redisMessageListenerContainer
        .addMessageListener(redisSagaEventConsumer, channelTopic());

    return redisMessageListenerContainer;
  }

  @Bean
  MessagePublisher redisMessagePublisher(RedisTemplate<String, Object> redisTemplate) {
    return new RedisMessagePublisher(redisTemplate, channelTopic());
  }

  @Bean
  ChannelTopic channelTopic() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("build channel topic = [{}]", topic);
    }
    return new ChannelTopic(topic);
  }

  @Bean
  public ActorEventChannel redisEventChannel(MetricsService metricsService,
      @Lazy RedisMessagePublisher redisMessagePublisher) {
    return new RedisActorEventChannel(metricsService, redisMessagePublisher);
  }

}
