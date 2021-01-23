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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.servicecomb.pack.alpha.fsm.channel.kafka.KafkaChannelAutoConfiguration;
import org.apache.servicecomb.pack.alpha.fsm.channel.memory.MemoryChannelAutoConfiguration;
import org.apache.servicecomb.pack.alpha.fsm.channel.rabbit.RabbitChannelAutoConfiguration;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.RedisChannelAutoConfiguration;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.repository.NoneTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.channel.DefaultTransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch.ElasticsearchTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.AkkaConfigPropertyAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

@Configuration
@ImportAutoConfiguration({
    MemoryChannelAutoConfiguration.class,
    KafkaChannelAutoConfiguration.class,
    RedisChannelAutoConfiguration.class, RabbitChannelAutoConfiguration.class})
@ConditionalOnProperty(value = {"alpha.feature.akka.enabled"})
public class FsmAutoConfiguration {

  // TODO
  //  Size of bulk request, When this value is greater than 0, the batch data will be lost when the jvm crashes.
  //  In the future, we can use Kafka to solve this problem instead of storing it directly in the ES.
  @Value("${alpha.feature.akka.transaction.repository.elasticsearch.batchSize:100}")
  int repositoryElasticsearchBatchSize;

  @Value("${alpha.feature.akka.transaction.repository.elasticsearch.refreshTime:5000}")
  int repositoryElasticsearchRefreshTime;

  @PostConstruct
  void init() {
    System.setProperty("es.set.netty.runtime.available.processors", "false");
  }

  @Bean
  public ActorSystem actorSystem(ConfigurableApplicationContext applicationContext,
      ConfigurableEnvironment environment, MetricsService metricsService,
      TransactionRepositoryChannel repositoryChannel) {
    ActorSystem system = ActorSystem
        .create("alpha-cluster", akkaConfiguration(applicationContext, environment));

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

  @Bean(name = "sagaShardRegionActor")
  public ActorRef sagaShardRegionActor(ActorSystem actorSystem) {
    return actorSystem.actorOf(Props.create(SagaShardRegionActor.class));
  }

  @Bean
  @ConditionalOnMissingBean(TransactionRepository.class)
  public TransactionRepository transactionRepository() {
    return new NoneTransactionRepository();
  }

  @Bean
  @ConditionalOnProperty(value = "alpha.feature.akka.transaction.repository.type", havingValue = "elasticsearch")
  public TransactionRepository transactionRepository(MetricsService metricsService,
      ElasticsearchTemplate template) {
    return new ElasticsearchTransactionRepository(template, metricsService,
        repositoryElasticsearchBatchSize, repositoryElasticsearchRefreshTime);
  }

  @Bean
  TransactionRepositoryChannel memoryTransactionRepositoryChannel(TransactionRepository repository,
      MetricsService metricsService) {
    return new DefaultTransactionRepositoryChannel(repository, metricsService);
  }

}
