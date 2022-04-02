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

import com.google.common.eventbus.EventBus;
import io.grpc.BindableService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.servicecomb.pack.alpha.core.CompositeOmegaCallback;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.PendingTaskRunner;
import org.apache.servicecomb.pack.alpha.core.PushBackOmegaCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EntityScan(basePackages = "org.apache.servicecomb.pack.alpha")
@Configuration
public class AlphaConfig {

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
  ScheduledExecutorService compensationScheduler() {
    return scheduler;
  }

  @Bean
  NodeStatus nodeStatus() {
    if (masterEnabled) {
      return new NodeStatus(NodeStatus.TypeEnum.SLAVE);
    } else {
      return new NodeStatus(NodeStatus.TypeEnum.MASTER);
    }
  }

  @Bean
  ServerStartable serverStartableWithAkka(GrpcServerConfig serverConfig,
      @Qualifier("alphaEventBus") EventBus eventBus, List<BindableService> bindableServices)
      throws IOException {
    ServerStartable bootstrap = new GrpcStartable(serverConfig, eventBus,
        bindableServices.toArray(new BindableService[0]));
    new Thread(bootstrap::start).start();
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
