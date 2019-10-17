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

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
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

/**
 * use ServiceCombConcurrencyStrategy，threadLocal variables  can not be inheritable
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
        HystrixCommand<String> command = new TestCircuitBreakerCommand("testCircuitBreaker",
            omegaContext);
        String result = command.execute();
        //inheritable GlobalTxId
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
                  .withMaxQueueSize(10)
                  .withCoreSize(3)
                  .withMaximumSize(3)
          )

      );
      this.omegaContext = omegaContext;
    }

    @Override
    protected String run() {
      //return threadLocal variable
      return this.omegaContext.globalTxId();
    }
  }

}
