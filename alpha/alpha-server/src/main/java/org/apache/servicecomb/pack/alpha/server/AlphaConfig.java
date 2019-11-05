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

package org.apache.servicecomb.pack.alpha.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.google.common.eventbus.EventBus;
import org.apache.servicecomb.pack.alpha.core.*;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.server.fsm.GrpcSagaEventService;
import org.apache.servicecomb.pack.alpha.server.tcc.GrpcTccEventService;
import org.apache.servicecomb.pack.alpha.server.tcc.callback.TccPendingTaskRunner;
import org.apache.servicecomb.pack.alpha.server.tcc.service.TccEventScanner;
import org.apache.servicecomb.pack.alpha.server.tcc.service.TccTxEventService;
import org.apache.servicecomb.pack.common.AlphaMetaKeys;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EntityScan(basePackages = "org.apache.servicecomb.pack.alpha")
@Configuration
public class AlphaConfig {
  private static final Logger LOG = LoggerFactory.getLogger(AlphaConfig.class);
  private final BlockingQueue<Runnable> pendingCompensations = new LinkedBlockingQueue<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Value("${alpha.compensation.retry.delay:3000}")
  private int delay;

  @Value("${alpha.tx.timeout-seconds:600}")
  private int globalTxTimeoutSeconds;

  @Value("${alpha.cluster.master.enabled:false}")
  private boolean masterEnabled;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  ApplicationEventPublisher applicationEventPublisher;

  @Bean("alphaEventBus")
  EventBus alphaEventBus() {
    return new EventBus("alphaEventBus");
  }

  @Bean
  Map<String, Map<String, OmegaCallback>> omegaCallbacks() {
    return new ConcurrentHashMap<>();
  }

  @Bean
  OmegaCallback omegaCallback(Map<String, Map<String, OmegaCallback>> callbacks) {
    return new PushBackOmegaCallback(pendingCompensations, new CompositeOmegaCallback(callbacks));
  }
  
  @Bean
  TxEventRepository springTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    return new SpringTxEventRepository(eventRepo);
  }

  @Bean
  CommandRepository springCommandRepository(TxEventEnvelopeRepository eventRepo, CommandEntityRepository commandRepository) {
    return new SpringCommandRepository(eventRepo, commandRepository);
  }

  @Bean
  TxTimeoutRepository springTxTimeoutRepository(TxTimeoutEntityRepository timeoutRepo) {
    return new SpringTxTimeoutRepository(timeoutRepo);
  }

  @Bean
  ScheduledExecutorService compensationScheduler() {
    return scheduler;
  }

  @Bean
  NodeStatus nodeStatus (){
    if(masterEnabled){
      return new NodeStatus(NodeStatus.TypeEnum.SLAVE);
    }else{
      return new NodeStatus(NodeStatus.TypeEnum.MASTER);
    }
  }

  @Bean
  TxConsistentService txConsistentService(
      @Value("${alpha.event.pollingInterval:500}") int eventPollingInterval,
      @Value("${alpha.event.scanner.enabled:true}") boolean eventScannerEnabled,
      ScheduledExecutorService scheduler,
      TxEventRepository eventRepository,
      CommandRepository commandRepository,
      TxTimeoutRepository timeoutRepository,
      OmegaCallback omegaCallback,
      NodeStatus nodeStatus) {
        if (eventScannerEnabled) {
          new EventScanner(scheduler,
              eventRepository, commandRepository, timeoutRepository,
              omegaCallback, eventPollingInterval, nodeStatus).run();
          LOG.info("Starting the EventScanner.");
          }
        TxConsistentService consistentService = new TxConsistentService(eventRepository);
        return consistentService;
  }

  @Bean
  TccPendingTaskRunner tccPendingTaskRunner() {
    return new TccPendingTaskRunner(delay);
  }

  @Bean
  GrpcTccEventService grpcTccEventService(TccTxEventService tccTxEventService) {
    return new GrpcTccEventService(tccTxEventService);
  }

  @Bean
  TccEventScanner tccEventScanner(TccTxEventService tccTxEventService) {
    return new TccEventScanner(tccTxEventService, delay, globalTxTimeoutSeconds);
  }

  @Bean
  @ConditionalOnProperty(name = "alpha.feature.akka.enabled", havingValue = "false", matchIfMissing = true)
  ServerStartable serverStartable(GrpcServerConfig serverConfig, TxConsistentService txConsistentService,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks, GrpcTccEventService grpcTccEventService,
      TccPendingTaskRunner tccPendingTaskRunner, TccEventScanner tccEventScanner, @Qualifier("alphaEventBus") EventBus eventBus) throws IOException {
    ServerMeta serverMeta = ServerMeta.newBuilder()
        .putMeta(AlphaMetaKeys.AkkaEnabled.name(), String.valueOf(false)).build();
    ServerStartable bootstrap = new GrpcStartable(serverConfig, eventBus,
        new GrpcTxEventEndpointImpl(txConsistentService, omegaCallbacks, serverMeta), grpcTccEventService);
    new Thread(bootstrap::start).start();
    tccPendingTaskRunner.start();
    tccEventScanner.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      tccPendingTaskRunner.shutdown();
      tccEventScanner.shutdown();
    }));
    LOG.info("alpha.feature.akka.enabled=false");
    return bootstrap;
  }

  @Bean
  @ConditionalOnProperty(name= "alpha.feature.akka.enabled", havingValue = "true")
  ServerStartable serverStartableWithAkka(GrpcServerConfig serverConfig,
      Map<String, Map<String, OmegaCallback>> omegaCallbacks, GrpcTccEventService grpcTccEventService,
      TccPendingTaskRunner tccPendingTaskRunner, TccEventScanner tccEventScanner, @Qualifier("alphaEventBus") EventBus eventBus, ActorEventChannel actorEventChannel) throws IOException {
    ServerMeta serverMeta = ServerMeta.newBuilder()
        .putMeta(AlphaMetaKeys.AkkaEnabled.name(), String.valueOf(true)).build();
    ServerStartable bootstrap = new GrpcStartable(serverConfig, eventBus,
        new GrpcSagaEventService(actorEventChannel, omegaCallbacks, serverMeta), grpcTccEventService);
    new Thread(bootstrap::start).start();
    tccPendingTaskRunner.start();
    tccEventScanner.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      tccPendingTaskRunner.shutdown();
      tccEventScanner.shutdown();
    }));
    LOG.info("alpha.feature.akka.enabled=true");
    return bootstrap;
  }

  @PostConstruct
  void init() {
    //https://github.com/elastic/elasticsearch/issues/25741
    System.setProperty("es.set.netty.runtime.available.processors", "false");
    new PendingTaskRunner(pendingCompensations, delay).run();
  }

  @PreDestroy
  void shutdown() {
    scheduler.shutdownNow();
  }
}
