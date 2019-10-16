/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.omega.transport.feign.hystrix;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 使用hystrix情况下，自动注册ServiceCombConcurrencyStrategy
 * @author : qixiangyu18@163.com
 */
@Configuration
@Order
@ConditionalOnClass({Hystrix.class})
public class HystrixServiceCombAutoConfiguration {

	@Autowired
	Optional<List<HystrixCallableWrapper>> hystrixCallableWrappers;

	@PostConstruct
	public void init() {
		// Keeps references of existing Hystrix plugins.
		HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
				.getEventNotifier();
		HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
				.getMetricsPublisher();
		HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
				.getPropertiesStrategy();
		HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance()
				.getCommandExecutionHook();
		HystrixConcurrencyStrategy concurrencyStrategy = detectRegisteredConcurrencyStrategy();

		HystrixPlugins.reset();

		// Registers existing plugins excepts the Concurrent Strategy plugin.
		HystrixPlugins.getInstance().registerConcurrencyStrategy(
				new ServiceCombConcurrencyStrategy(concurrencyStrategy,hystrixCallableWrappers));
		HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
		HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
		HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
		HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
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
