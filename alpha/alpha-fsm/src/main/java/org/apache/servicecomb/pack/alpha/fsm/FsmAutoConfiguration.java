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

import static org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER;
import static org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SpringAkkaExtension.SPRING_EXTENSION_PROVIDER;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import javax.annotation.PostConstruct;
import org.apache.servicecomb.pack.alpha.fsm.channel.ActiveMQActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.MessagePublisher;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.RedisMessagePublisher;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.RedisMessageSubscriber;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.repository.NoneTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch.ElasticsearchTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.channel.MemoryTransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.sink.ActorEventSink;
import org.apache.servicecomb.pack.alpha.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.channel.KafkaActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.channel.MemoryActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.channel.RedisActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.sink.SagaActorEventSender;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.AkkaConfigPropertyAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

@Configuration
@ConditionalOnProperty(value = {"alpha.feature.akka.enabled"})
public class FsmAutoConfiguration {

  @Value("${alpha.feature.akka.channel.memory.size:-1}")
  int memoryEventChannelMemorySize;

  @Value("${alpha.feature.akka.transaction.repository.elasticsearch.batchSize:100}")
  int repositoryElasticsearchBatchSize;

  @Value("${alpha.feature.akka.transaction.repository.elasticsearch.refreshTime:5000}")
  int repositoryElasticsearchRefreshTime;

  @Value("${alpha.feature.akka.transaction.repository.elasticsearch.memory.size:-1}")
  int memoryTransactionRepositoryChannelSize;

  @PostConstruct
  void init() {
    System.setProperty("es.set.netty.runtime.available.processors", "false");
  }

  @Bean
  public ActorSystem actorSystem(ConfigurableApplicationContext applicationContext,
      ConfigurableEnvironment environment, MetricsService metricsService,
      TransactionRepositoryChannel repositoryChannel) {
    ActorSystem system = ActorSystem
        .create("alpha-akka", akkaConfiguration(applicationContext, environment));
    SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
    SAGA_DATA_EXTENSION_PROVIDER.get(system).setRepositoryChannel(repositoryChannel);
    SAGA_DATA_EXTENSION_PROVIDER.get(system).setMetricsService(metricsService);
    return system;
  }

  @Bean
  public Config akkaConfiguration(ConfigurableApplicationContext applicationContext,
      ConfigurableEnvironment environment) {
    final Map<String, Object> converted = AkkaConfigPropertyAdapter.getPropertyMap(environment);
    return ConfigFactory.parseMap(converted)
        .withFallback(ConfigFactory.defaultReference(applicationContext.getClassLoader()));
  }

  @Bean
  public MetricsService metricsService() {
    return new MetricsService();
  }

  @Bean
  public ActorEventSink actorEventSink(MetricsService metricsService) {
    return new SagaActorEventSender(metricsService);
  }

  @Bean
  @ConditionalOnMissingBean(ActorEventChannel.class)
  public ActorEventChannel memoryEventChannel(ActorEventSink actorEventSink,
      MetricsService metricsService) {
    return new MemoryActorEventChannel(actorEventSink, memoryEventChannelMemorySize,
        metricsService);
  }

  @Bean
  @ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "activemq")
  public ActorEventChannel activeMqEventChannel(ActorEventSink actorEventSink,
      MetricsService metricsService) {
    return new ActiveMQActorEventChannel(actorEventSink, metricsService);
  }

  @Bean
  @ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "kafka")
  public ActorEventChannel kafkaEventChannel(ActorEventSink actorEventSink,
      MetricsService metricsService) {
    return new KafkaActorEventChannel(actorEventSink, metricsService);
  }

  @Bean
  @ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "redis")
  public ActorEventChannel redisEventChannel(ActorEventSink actorEventSink, MetricsService metricsService, @Lazy RedisMessagePublisher redisMessagePublisher){
    return new RedisActorEventChannel(actorEventSink, metricsService, redisMessagePublisher);
  }

  @Configuration
  @ConditionalOnClass(RedisConnection.class)
  @ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "redis")
  public static class RedisConfig{
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${alpha.feature.akka.channel.redis.topic:servicecomb-pack-actor-event}")
    private String topic;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
      RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
      redisTemplate.setKeySerializer(new StringRedisSerializer());
      redisTemplate.setHashKeySerializer(new GenericToStringSerializer<>(Object.class));
      redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
      redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
      redisTemplate.setConnectionFactory(redisConnectionFactory);

      return redisTemplate;
    }

    @Bean
    RedisMessageSubscriber redisMessageSubscriber(ActorEventSink actorEventSink, NodeStatus nodeStatus){
      return new RedisMessageSubscriber(actorEventSink, nodeStatus);
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(ActorEventSink actorEventSink, NodeStatus nodeStatus){
      return new MessageListenerAdapter(redisMessageSubscriber(actorEventSink, nodeStatus));
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory, ActorEventSink actorEventSink, NodeStatus nodeStatus){
      RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();

      redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
      redisMessageListenerContainer.addMessageListener(redisMessageSubscriber(actorEventSink, nodeStatus), channelTopic());

      return redisMessageListenerContainer;
    }

    @Bean
    MessagePublisher redisMessagePublisher(RedisTemplate<String, Object> redisTemplate){
      return new RedisMessagePublisher(redisTemplate, channelTopic());
    }

    @Bean
    ChannelTopic channelTopic(){
      if(logger.isDebugEnabled()) {
        logger.debug("build channel topic = [{}]", topic);
      }
      return new ChannelTopic(topic);
    }
  }

  @Bean
  @ConditionalOnMissingBean(TransactionRepository.class)
  public TransactionRepository transcationRepository() {
    return new NoneTransactionRepository();
  }

  @Bean
  @ConditionalOnMissingBean(TransactionRepositoryChannel.class)
  @ConditionalOnProperty(value = "alpha.feature.akka.transaction.repository.channel.type", havingValue = "memory", matchIfMissing = true)
  TransactionRepositoryChannel memoryTransactionRepositoryChannel(TransactionRepository repository,
      MetricsService metricsService) {
    return new MemoryTransactionRepositoryChannel(repository, memoryTransactionRepositoryChannelSize,
        metricsService);
  }

  @Bean
  @ConditionalOnProperty(value = "alpha.feature.akka.transaction.repository.type", havingValue = "elasticsearch")
  public TransactionRepository transcationRepository(MetricsService metricsService,
      ElasticsearchTemplate template) {
    return new ElasticsearchTransactionRepository(template, metricsService,
        repositoryElasticsearchBatchSize, repositoryElasticsearchRefreshTime);
  }

}
