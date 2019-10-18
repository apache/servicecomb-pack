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

import com.netflix.hystrix.HystrixCommand;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.context.UniqueIdGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * when not use ServiceCombConcurrencyStrategy ,threadLocal variables can not be inheritable
 */

public class HystrixWithoutConcurrencyStrategyTests {

  private OmegaContext omegaContext = new OmegaContext(new UniqueIdGenerator());

  @Test
  public void testCircuitBreakerWithoutServiceCombConcurrencyStrategy() {

    for (int i = 0; i < 5; i++) {
      try {
        omegaContext.newGlobalTxId();
        HystrixCommand<String> command = new HystrixConcurrencyStrategyTests.TestCircuitBreakerCommand(
            "testCircuitBreaker", omegaContext);
        String result = command.execute();
        //after core thread all invoked (3 times) ,globalTxId can not be inheritable
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
