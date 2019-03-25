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

package org.apache.servicecomb.pack.alpha.server.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.List;
import org.apache.servicecomb.pack.alpha.core.event.GrpcStartableStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = {"spring.cloud.consul.enabled"})
@AutoConfigureAfter(ConsulAutoConfiguration.class)
public class AlphaConsulAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String consuleInstanceId;

  private int actualAlphaServerPort;

  @Value("${alpha.server.port}")
  private int alphaServerPort;

  @Value("${spring.application.name}")
  private String serviceName;

  @Autowired
  private ConsulClient consulClient;

  @Autowired
  @Qualifier("alphaEventBus")
  EventBus eventBus;

  @Autowired
  ConsulDiscoveryProperties consulDiscoveryProperties;

  @PostConstruct
  public void init(){
    eventBus.register(this);
    // Unregister from consul when shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        LOG.info("Unregister Consul {}", consuleInstanceId);
        consulClient.agentServiceDeregister(consuleInstanceId);
      }
    });
  }

  /**
   * Format local consul instanceId to instanceId of consul server
   * */
  private String formatConsulInstanceId(String consuleInstanceId) {
    return consuleInstanceId.replace(".", "-").replace(":", "-");
  }

  /**
   * Update actual grpc port to actualAlphaServerPort
   * */
  @Subscribe
  public void listenGrpcStartableStartedEvent(GrpcStartableStartedEvent grpcStartableStartedEvent) {
    if(alphaServerPort == 0){
      actualAlphaServerPort = grpcStartableStartedEvent.getPort();
    }
  }

  /**
   * Update grpc port of consul tags after Conusl Registered
   * */
  @EventListener
  public void listenInstanceRegisteredEvent(InstanceRegisteredEvent instanceRegisteredEvent){
    if(alphaServerPort == 0){
      if(instanceRegisteredEvent.getConfig() instanceof ConsulDiscoveryProperties){
        ConsulDiscoveryProperties properties = (ConsulDiscoveryProperties)instanceRegisteredEvent.getConfig();
        this.consuleInstanceId = formatConsulInstanceId(properties.getInstanceId());
        Response<List<CatalogService>> services = consulClient.getCatalogService(serviceName,null);
        if(services.getValue() != null){
          services.getValue().stream().filter(service ->
              service.getServiceId().equalsIgnoreCase(this.consuleInstanceId)).forEach(service -> {

            NewService newservice =  new NewService();
            newservice.setName(service.getServiceName());
            newservice.setId(service.getServiceId());
            List<String> tags = consulDiscoveryProperties.getTags();
            tags.remove("alpha-server-port=0");
            tags.add("alpha-server-port="+actualAlphaServerPort);
            newservice.setTags(tags);
            consulClient.agentServiceRegister(newservice);
          });
        }
      }
    }
  }
}
