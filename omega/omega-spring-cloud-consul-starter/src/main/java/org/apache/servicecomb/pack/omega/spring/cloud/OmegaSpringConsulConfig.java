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

/**
 * Get the access address of Alpah Server from Consul
 * Turn this feautre on by set alpha.cluster.register.type=consul
 * First omega gets the Alpha address from Consul with ${alpha.cluster.serviceId}
 * If omega can't get it in Consul then use ${alpha.cluster.address}
 */

package org.apache.servicecomb.pack.omega.spring.cloud;

import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(value = {"alpha.cluster.register.type"}, havingValue = "consul")
@AutoConfigureAfter(value = {ConsulAutoConfiguration.class})
class OmegaSpringConsulConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OmegaSpringConsulConfig.class);
    private static final String ALPHA_SERVER_HOST_KEY = "alpha-server-host";
    private static final String ALPHA_SERVER_PORT_KEY = "alpha-server-port";

    @Autowired
    public ConsulDiscoveryClient discoveryClient;

    @Bean
    AlphaClusterDiscovery alphaClusterAddress(
            @Value("${alpha.cluster.serviceId:servicecomb-alpha-server}") String serviceId,
            @Value("${alpha.cluster.address:0.0.0.0:8080}") String[] addresses) {
        StringBuffer eurekaServiceUrls = new StringBuffer();

        String[] alphaAddresses = this.getAlphaAddress(serviceId);
        if (alphaAddresses.length > 0) {
            AlphaClusterDiscovery alphaClusterDiscovery = AlphaClusterDiscovery.builder()
                    .discoveryType(AlphaClusterDiscovery.DiscoveryType.CONSUL)
                    .discoveryInfo(eurekaServiceUrls.toString())
                    .addresses(alphaAddresses)
                    .build();
            return alphaClusterDiscovery;
        } else {
            AlphaClusterDiscovery alphaClusterDiscovery = AlphaClusterDiscovery.builder()
                    .discoveryType(AlphaClusterDiscovery.DiscoveryType.DEFAULT)
                    .addresses(addresses)
                    .build();
            return alphaClusterDiscovery;
        }
    }

    private String[] getAlphaAddress(String serviceId) {
        List<String> alphaAddresses = new ArrayList<>();
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
        boolean foundAlphaServer = Boolean.FALSE;
        for (ServiceInstance serviceInstance : serviceInstances) {
            foundAlphaServer = Boolean.TRUE;
            if (serviceInstance.getMetadata().containsKey(ALPHA_SERVER_HOST_KEY) && serviceInstance.getMetadata().containsKey(ALPHA_SERVER_PORT_KEY)) {
                alphaAddresses.add(serviceInstance.getMetadata().get(ALPHA_SERVER_HOST_KEY).trim()+":"+serviceInstance.getMetadata().get(ALPHA_SERVER_PORT_KEY).trim());
            }else{
                LOG.warn("Ignore alpha instance {}, No tag {} or {} found ",serviceInstance.getServiceId(),ALPHA_SERVER_HOST_KEY,ALPHA_SERVER_PORT_KEY);
            }
        }
        if (foundAlphaServer) {
            if (alphaAddresses.size() == 0) {
                LOG.warn("Alpha has been found in Consul, " +
                        "but Alpha's registered address information is not found in Consul instance tags. " +
                        "Please check Alpha is configured spring.cloud.consul.enabled=true and version 0.4.0+");
            }
        } else {
            LOG.warn("No Alpha Server {} found in the Consul", serviceId);
        }
        return alphaAddresses.toArray(new String[alphaAddresses.size()]);
    }
}
