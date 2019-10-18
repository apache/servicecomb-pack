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

package org.apache.servicecomb.pack.omega.transport.hystrix;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

/**
 * if use hystrix ,auto configuration ServiceCombConcurrencyStrategy
 * <p>
 * see org.springframework.cloud.netflix.hystrix.security.HystrixSecurityAutoConfigurations
 */
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnClass({Hystrix.class})
public class HystrixServiceCombAutoConfiguration {

  private static final Log log = LogFactory.getLog(HystrixServiceCombAutoConfiguration.class);

  @Autowired(required = false)
  List<HystrixCallableWrapper> hystrixCallableWrappers;

  @PostConstruct
  public void init() {
    try {
      if (CollectionUtils.isEmpty(hystrixCallableWrappers)) {
        log.info(
            "no hystrixCallableWrapper find ,ServiceCombConcurrencyStrategy ignore Configuration");
        return;
      }
      HystrixConcurrencyStrategy concurrencyStrategy = detectRegisteredConcurrencyStrategy();
      if (concurrencyStrategy instanceof ServiceCombConcurrencyStrategy) {
        log.info(
            "Current Hystrix plugins concurrencyStrategy is ServiceCombConcurrencyStrategy ignore Configuration");
        return;
      }

      // Keeps references of existing Hystrix plugins.
      HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
          .getEventNotifier();
      HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
          .getMetricsPublisher();
      HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
          .getPropertiesStrategy();
      HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance()
          .getCommandExecutionHook();

      log.info("Current Hystrix plugins configuration is ["
          + "concurrencyStrategy [" + concurrencyStrategy + "]," + "eventNotifier ["
          + eventNotifier + "]," + "metricPublisher [" + metricsPublisher + "],"
          + "propertiesStrategy [" + propertiesStrategy + "]," + "]");
      HystrixPlugins.reset();

      // Registers existing plugins excepts the Concurrent Strategy plugin.
      HystrixPlugins.getInstance().registerConcurrencyStrategy(
          new ServiceCombConcurrencyStrategy(concurrencyStrategy, hystrixCallableWrappers));
      HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
      HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
      HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
      HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);

      log.info("Succeeded to register ServiceComb Hystrix Concurrency Strategy");

    } catch (Exception e) {
      log.error("Failed to register ServiceComb Hystrix Concurrency Strategy", e);
    }

  }

  private HystrixConcurrencyStrategy detectRegisteredConcurrencyStrategy() {
    return HystrixPlugins.getInstance()
        .getConcurrencyStrategy();
  }

  @Bean
  @ConditionalOnBean(OmegaContext.class)
  public OmegaContextCallableWrapper omegaContextCallableWrapper(OmegaContext context) {
    return new OmegaContextCallableWrapper(context);
  }

}
