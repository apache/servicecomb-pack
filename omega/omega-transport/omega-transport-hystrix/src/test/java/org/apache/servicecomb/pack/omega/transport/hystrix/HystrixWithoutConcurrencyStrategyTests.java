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

import com.netflix.hystrix.HystrixCommand;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.UniqueIdGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * 默认策略下，omegaContext的线程变量无法正确传递
 */

public class HystrixWithoutConcurrencyStrategyTests {

    private OmegaContext omegaContext = new OmegaContext(new UniqueIdGenerator());

    @Test
    public void testCircuitBreakerWithoutServiceCombConcurrencyStrategy() {

        for (int i = 0; i < 5; i++) {
            try {
                omegaContext.newGlobalTxId();
                HystrixCommand<String> command = new HystrixConcurrencyStrategyTests.TestCircuitBreakerCommand("testCircuitBreaker", omegaContext);
                String result = command.execute();
                //因为线程池核心数是3，所以第三次以后globalTxId将不一致
                if (i > 2) {
                    Assert.assertNotEquals(result, omegaContext.globalTxId());
                } else {
                    Assert.assertEquals(result, omegaContext.globalTxId());
                }
            } finally {
                omegaContext.clear();
            }
        }
    }


}
