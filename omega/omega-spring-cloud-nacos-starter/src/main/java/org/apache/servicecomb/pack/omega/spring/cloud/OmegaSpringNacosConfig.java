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
 * Get the access address of Alpah Server from Nacos Server
 * Turn this feautre on by set alpha.cluster.register.type=nacos
 * First omega gets the Alpha address from Nacos with ${alpha.cluster.serviceId}
 * If omega can't get it in Nacos then use ${alpha.cluster.address}
 */

package org.apache.servicecomb.pack.omega.spring.cloud;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@ConditionalOnProperty(value = {"alpha.cluster.register.type"}, havingValue = "nacos")
@AutoConfigureAfter(value = {NacosDiscoveryAutoConfiguration.class})
class OmegaSpringNacosConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OmegaSpringNacosConfig.class);

    @Autowired
    public DiscoveryClient discoveryClient;

    @Bean
    AlphaClusterDiscovery alphaClusterAddress(
            @Value("${alpha.cluster.serviceId:servicecomb-alpha-server}") String serviceId,
            @Value("${alpha.cluster.address:0.0.0.0:8080}") String[] addresses) {
        StringBuffer serviceUrls = new StringBuffer();

        String[] alphaAddresses = this.getAlphaAddress(serviceId);

        LOG.info("alphaAddress = {}", Arrays.toString(alphaAddresses));

        if (alphaAddresses.length > 0) {
            AlphaClusterDiscovery alphaClusterDiscovery = AlphaClusterDiscovery.builder()
                    .discoveryType(AlphaClusterDiscovery.DiscoveryType.NACOS)
                    .discoveryInfo(serviceUrls.toString())
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
            if (serviceInstance.getMetadata().containsKey(serviceId)) {
                String alphaAddress = serviceInstance.getMetadata().get(serviceId);
                alphaAddresses.add(alphaAddress);
            }
        }
        if (foundAlphaServer) {
            if (alphaAddresses.size() == 0) {
                LOG.warn("Alpha has been found in Nacos, " +
                        "but Alpha's registered address information is not found in Nacos instance metadata. " +
                        "Please check Alpha is configured spring.profiles.active=spring-cloud");
            }
        } else {
            LOG.warn("No Alpha Server {} found in the Nacos", serviceId);
        }
        return alphaAddresses.toArray(new String[alphaAddresses.size()]);
    }
}
