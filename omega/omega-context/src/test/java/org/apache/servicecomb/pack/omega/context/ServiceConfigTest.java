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
package org.apache.servicecomb.pack.omega.context;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class ServiceConfigTest {
  final String LONG_NAME =  "ABCDEFGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABCDEFG"
      + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
      + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
      + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
      + "ABCDEFGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  final String SERVICE_NAME = "Service";
  final String INSTANCE_NAME = "Instance";

  @Test
  public void serviceConfigWithoutServiceInstance() {
    ServiceConfig serviceConfig = new ServiceConfig(SERVICE_NAME);
    serviceConfig.serviceName();
    assertThat(serviceConfig.serviceName(), is(SERVICE_NAME));
    assertTrue(serviceConfig.instanceId().startsWith(SERVICE_NAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void serviceConfigWithLongServiceName() {
    ServiceConfig serviceConfig = new ServiceConfig(LONG_NAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void serviceConfigWithLongServiceInstanceName() {
    ServiceConfig serviceConfig = new ServiceConfig(SERVICE_NAME, LONG_NAME);
  }
}
