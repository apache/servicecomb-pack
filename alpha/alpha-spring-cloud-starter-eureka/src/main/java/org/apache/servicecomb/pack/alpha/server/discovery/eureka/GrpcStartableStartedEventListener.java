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

package org.apache.servicecomb.pack.alpha.server.discovery.eureka;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.servicecomb.pack.alpha.core.event.GrpcStartableStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

/**
 * Listen for GrpcStartableStartedEvent and initialize the Eureka instance metadata
 * */

@Component
@ConditionalOnClass({EurekaClientAutoConfiguration.class})
@ConditionalOnProperty(value = {"eureka.client.enabled"}, matchIfMissing = false)
public class GrpcStartableStartedEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String ALPHA_SERVER_GRPC_ADDRESS_KEY = "servicecomb-alpha-server";

  @Autowired
  @Qualifier("alphaEventBus")
  EventBus eventBus;

  @Autowired(required = false)
  public EurekaInstanceConfigBean eurekaInstanceConfigBean;

  @PostConstruct
  public void init(){
    eventBus.register(this);
  }

  /**
   * Update grpc port to eureka instance metadata
   * */
  @Subscribe
  public void listenGrpcStartableStartedEvent(GrpcStartableStartedEvent grpcStartableStartedEvent) {
    if(eurekaInstanceConfigBean!=null && this.eurekaInstanceConfigBean.getMetadataMap().containsKey(ALPHA_SERVER_GRPC_ADDRESS_KEY)){
      String grpcAddressValue = this.eurekaInstanceConfigBean.getMetadataMap().get(ALPHA_SERVER_GRPC_ADDRESS_KEY);
      if(grpcAddressValue!=null && grpcAddressValue.endsWith(":0")){
        grpcAddressValue = grpcAddressValue.replace(":0",":"+grpcStartableStartedEvent.getPort());
        this.eurekaInstanceConfigBean.getMetadataMap().put(ALPHA_SERVER_GRPC_ADDRESS_KEY,grpcAddressValue);
        LOG.info("Register grpc address {} to Eureka instance metadata",grpcAddressValue);
      }
    }
  }
}
