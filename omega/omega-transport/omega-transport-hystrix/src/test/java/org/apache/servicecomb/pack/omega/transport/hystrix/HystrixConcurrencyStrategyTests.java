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

package org.apache.servicecomb.pack.omega.transport.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用ServiceCombConcurrencyStrategy，omegaContext的线程变量能正确传递
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(classes = HystrixTestApplication.class)
public class HystrixConcurrencyStrategyTests {

    @Autowired
    private OmegaContext omegaContext;

    @Test
    public void testConcurrencyStrategyInstalled() {

        HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance()
                .getConcurrencyStrategy();
        assertThat(concurrencyStrategy)
                .isInstanceOf(ServiceCombConcurrencyStrategy.class);
    }

    @Test
    public void testCircuitBreaker() {
        for (int i = 0; i < 5; i++) {
            try {
                omegaContext.newGlobalTxId();
                HystrixCommand<String> command = new TestCircuitBreakerCommand("testCircuitBreaker", omegaContext);
                String result = command.execute();
                //与父线程的GlobalTxId一致,LocalTxId同理
                Assert.assertEquals(result, omegaContext.globalTxId());
            } finally {
                omegaContext.clear();
            }
        }
    }


    public static class TestCircuitBreakerCommand extends HystrixCommand<String> {

        private final OmegaContext omegaContext;

        public TestCircuitBreakerCommand(String name, OmegaContext omegaContext) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ThreadPoolTestGroup"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("testCommandKey"))
                    .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(name))
                    .andThreadPoolPropertiesDefaults(
                            HystrixThreadPoolProperties.Setter()
                                    .withMaxQueueSize(10)   //配置队列大小
                                    .withCoreSize(3)    // 配置线程池里的线程数
                                    .withMaximumSize(3) // 与coreSize一致确保不会创建额外线程，方便观察
                    )

            );
            this.omegaContext = omegaContext;
        }

        @Override
        protected String run() {
            //返回线程变量
            return this.omegaContext.globalTxId();
        }
    }

}
