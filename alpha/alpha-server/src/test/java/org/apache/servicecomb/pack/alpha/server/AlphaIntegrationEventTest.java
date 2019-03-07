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
import org.apache.servicecomb.pack.alpha.core.event.GrpcStartableStartedEvent;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.apache.servicecomb.pack.alpha.server.discovery.eureka.GrpcStartableStartedEventListener.ALPHA_SERVER_GRPC_ADDRESS_KEY;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8080",
        "eureka.client.enabled=true"
       })
public class AlphaIntegrationEventTest {

  @Autowired
  private EventBus eventBus;

  @MockBean
  private EurekaInstanceConfigBean eurekaInstanceConfigBean;

  @Test
  public void eurekaInstanceConfigBeanPortUpdater(){
    when(eurekaInstanceConfigBean.getMetadataMap()).thenReturn(new HashMap<>());
    this.eurekaInstanceConfigBean.getMetadataMap().put(ALPHA_SERVER_GRPC_ADDRESS_KEY,"0.0.0.0:0");
    eventBus.post(new GrpcStartableStartedEvent(9000));
    assertThat(eurekaInstanceConfigBean.getMetadataMap().get(ALPHA_SERVER_GRPC_ADDRESS_KEY),is("0.0.0.0:9000"));
  }

}
