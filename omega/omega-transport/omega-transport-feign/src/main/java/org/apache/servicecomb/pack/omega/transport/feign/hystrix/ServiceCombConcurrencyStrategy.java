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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

/**
 * HystrixConcurrencyStrategy 代理，开放接口实现扩展包装wrapCallable，便于传递线程变量
 * @author : qixiangyu18@163.com
 */
public class ServiceCombConcurrencyStrategy extends HystrixConcurrencyStrategy {

	private HystrixConcurrencyStrategy existingConcurrencyStrategy;
	private List<HystrixCallableWrapper> hystrixCallableWrappers;

	public ServiceCombConcurrencyStrategy(
			HystrixConcurrencyStrategy existingConcurrencyStrategy,
			Optional<List<HystrixCallableWrapper>> optionalHystrixCallableWrappers) {
		this.existingConcurrencyStrategy = existingConcurrencyStrategy;
		this.hystrixCallableWrappers = optionalHystrixCallableWrappers.orElseGet(Collections::emptyList);
	}

	@Override
	public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
		return existingConcurrencyStrategy != null
				? existingConcurrencyStrategy.getBlockingQueue(maxQueueSize)
				: super.getBlockingQueue(maxQueueSize);
	}

	@Override
	public <T> HystrixRequestVariable<T> getRequestVariable(
			HystrixRequestVariableLifecycle<T> rv) {
		return existingConcurrencyStrategy != null
				? existingConcurrencyStrategy.getRequestVariable(rv)
				: super.getRequestVariable(rv);
	}

	@Override
	public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey,
			HystrixProperty<Integer> corePoolSize,
			HystrixProperty<Integer> maximumPoolSize,
			HystrixProperty<Integer> keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		return existingConcurrencyStrategy != null
				? existingConcurrencyStrategy.getThreadPool(threadPoolKey, corePoolSize,
						maximumPoolSize, keepAliveTime, unit, workQueue)
				: super.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize,
						keepAliveTime, unit, workQueue);
	}

	@Override
	public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey,
			HystrixThreadPoolProperties threadPoolProperties) {
		return existingConcurrencyStrategy != null
				? existingConcurrencyStrategy.getThreadPool(threadPoolKey,
						threadPoolProperties)
				: super.getThreadPool(threadPoolKey, threadPoolProperties);
	}

	@Override
	public <T> Callable<T> wrapCallable(Callable<T> callable) {
		//Invoke custom callable wrapper
		for(HystrixCallableWrapper callableWrapper : hystrixCallableWrappers){
			if(callableWrapper.shouldWrap()){
				callable = callableWrapper.wrapCallable(callable);
			}
		}
		return existingConcurrencyStrategy != null
				? existingConcurrencyStrategy
						.wrapCallable(callable)
				: super.wrapCallable(callable);
	}

}
