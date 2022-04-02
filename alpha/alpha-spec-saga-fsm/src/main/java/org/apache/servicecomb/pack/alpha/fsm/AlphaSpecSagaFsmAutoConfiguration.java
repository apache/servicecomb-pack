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
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.apache.servicecomb.pack.alpha.fsm.api.APIv1Controller;
import org.apache.servicecomb.pack.alpha.fsm.api.APIv1Impl;
import org.apache.servicecomb.pack.alpha.fsm.channel.AbstractEventConsumer;
import org.apache.servicecomb.pack.alpha.fsm.channel.memory.MemoryActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.channel.memory.MemorySagaEventConsumer;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.metrics.AlphaMetricsEndpointImpl;
import org.apache.servicecomb.pack.alpha.fsm.properties.SpecSagaAkkaProperties;
import org.apache.servicecomb.pack.alpha.fsm.repository.NoneTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.repository.channel.DefaultTransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch.ElasticsearchTransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.AkkaConfigPropertyAdapter;
import org.apache.servicecomb.pack.alpha.fsm.test.FsmSagaDataController;
import org.apache.servicecomb.pack.common.AlphaMetaKeys;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@Configuration
@ImportAutoConfiguration({SpecSagaAkkaProperties.class})
@ConditionalOnExpression("'${alpha.spec.names}'.contains('saga-akka')")
public class AlphaSpecSagaFsmAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public AlphaSpecSagaFsmAutoConfiguration() {
    System.setProperty("es.set.netty.runtime.available.processors", "false");
    LOG.info("Alpha Specification Saga Akka");
  }

  @Bean
  public MetricsService metricsService() {
    return new MetricsService();
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

  @Bean(name = "sagaShardRegionActor")
  public ActorRef sagaShardRegionActor(ActorSystem actorSystem) {
    return actorSystem.actorOf(Props.create(SagaShardRegionActor.class));
  }

  @Bean
  public ElasticsearchRestTemplate elasticsearchRestTemplate(
      SpecSagaAkkaProperties specSagaAkkaProperties) {
    HttpHost[] hosts = Arrays.stream(
            specSagaAkkaProperties.getRepository().getElasticsearch().getUris()
                .split(","))
        .map(uri -> HttpHost.create(uri))
        .toArray(HttpHost[]::new);
    RestClientBuilder builder = RestClient.builder(hosts);
    RestHighLevelClient client = new RestHighLevelClient(builder);
    return new ElasticsearchRestTemplate(client);
  }

  @Bean
  public TransactionRepository transactionRepository(
      SpecSagaAkkaProperties specSagaAkkaProperties,
      ElasticsearchRestTemplate elasticsearchRestTemplate,
      MetricsService metricsService) {
    if (specSagaAkkaProperties.getRepository().getName().equals("elasticsearch")) {
      return new ElasticsearchTransactionRepository(specSagaAkkaProperties.getRepository()
          .getElasticsearch(), elasticsearchRestTemplate, metricsService);
    } else {
      return new NoneTransactionRepository();
    }
  }

  @Bean
  TransactionRepositoryChannel memoryTransactionRepositoryChannel(TransactionRepository repository,
      MetricsService metricsService) {
    return new DefaultTransactionRepositoryChannel(repository, metricsService);
  }

  @Bean
  @ConditionalOnMissingBean(ActorEventChannel.class)
  public ActorEventChannel eventChannel(SpecSagaAkkaProperties specSagaAkkaProperties,
      MetricsService metricsService) {
    if (specSagaAkkaProperties.getChannel().getName().equals("memory")) {
      return new MemoryActorEventChannel(metricsService,
          specSagaAkkaProperties.getChannel().getMemory().getMaxLength());
    } else {
      return new MemoryActorEventChannel(metricsService,
          specSagaAkkaProperties.getChannel().getMemory().getMaxLength());
    }
  }

  @Bean
  AbstractEventConsumer eventConsumer(SpecSagaAkkaProperties specSagaAkkaProperties,
      ActorSystem actorSystem,
      @Qualifier("sagaShardRegionActor") ActorRef sagaShardRegionActor,
      MetricsService metricsService,
      ActorEventChannel actorEventChannel) {
    if (specSagaAkkaProperties.getChannel().getName().equals("memory")) {
      return new MemorySagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService,
          (MemoryActorEventChannel) actorEventChannel);
    } else {
      return new MemorySagaEventConsumer(actorSystem, sagaShardRegionActor, metricsService,
          (MemoryActorEventChannel) actorEventChannel);
    }
  }

  @Bean
  GrpcSagaEventService grpcSagaEventService(ActorEventChannel actorEventChannel, Map<String, Map<String, OmegaCallback>> omegaCallbacks){
    ServerMeta serverMeta = ServerMeta.newBuilder()
        .putMeta(AlphaMetaKeys.AkkaEnabled.name(), String.valueOf(true)).build();
    return new GrpcSagaEventService(actorEventChannel, omegaCallbacks, serverMeta);
  }

  @Bean
  APIv1Impl apIv1(){
    return new APIv1Impl();
  }

  @Bean
  APIv1Controller apIv1Controller(){
    return new APIv1Controller();
  }

  @Bean
  AlphaMetricsEndpoint alphaMetricsEndpoint(){
    return new AlphaMetricsEndpointImpl();
  }

  @Bean
  @Profile("test")
  FsmSagaDataController fsmSagaDataController(){
    return new FsmSagaDataController();
  }
}
