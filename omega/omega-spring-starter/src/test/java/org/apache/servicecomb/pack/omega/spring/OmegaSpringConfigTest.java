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

package org.apache.servicecomb.pack.omega.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import javax.annotation.Resource;
import org.apache.servicecomb.pack.omega.spring.properties.BootAlphaClusterProperties;
import org.apache.servicecomb.pack.omega.spring.properties.BootOmegaClientProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmegaSpringConfigTest.class)
@SpringBootApplication
public class OmegaSpringConfigTest {

  @Resource
  private BootAlphaClusterProperties alphaClusterProperties;

  @Resource
  private BootOmegaClientProperties omegaClientProperties;

  @Test
  public void assertAlphaClusterProperties() {
    assertThat(alphaClusterProperties.getAddress(), is(Arrays.asList("test-01:8080", "test-02:8080")));
    assertFalse(alphaClusterProperties.getSsl().isEnableSSL());
    assertFalse(alphaClusterProperties.getSsl().isMutualAuth());
    assertThat(alphaClusterProperties.getSsl().getCert(), is("cert"));
    assertThat(alphaClusterProperties.getSsl().getCertChain(), is("certChain"));
    assertThat(alphaClusterProperties.getSsl().getKey(), is("key"));
    assertThat(omegaClientProperties.getReconnectDelayMilliSeconds(), is(60000L));
    assertThat(omegaClientProperties.getTimeoutSeconds(), is(10L));
  }
}
