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
 * Get the access address of Alpah Server from Eureka Server
 * Turn this feautre on by set alpha.cluster.register.type=spring-cloud
 * First omega gets the Alpha address from Eureka with ${alpha.cluster.serviceId}
 * If omega can't get it in Eureka then use ${alpha.cluster.address}
 */
package org.apache.servicecomb.pack.omega.spring.cloud;

import com.google.common.collect.ImmutableList;
import com.netflix.discovery.EurekaClientConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.format.KryoMessageFormat;
import org.apache.servicecomb.pack.omega.format.MessageFormat;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(value = {"eureka.client.enabled"}, matchIfMissing = true)
@AutoConfigureAfter(value = {EurekaDiscoveryClientConfiguration.class})
class OmegaSpringEurekaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OmegaSpringEurekaConfig.class);

    @Autowired
    public DiscoveryClient discoveryClient;

    @Autowired
    public EurekaClientConfig eurekaClientConfig;

    @Bean(name = {"alphaClusterEurekaConfig"})
    @ConditionalOnProperty(name = "alpha.cluster.register.type", havingValue = "spring-cloud")
    AlphaClusterConfig alphaClusterEurekaConfig(
            @Value("${alpha.cluster.address:#{null}}") String[] addresses,
            @Value("${alpha.cluster.serviceId:servicecomb-alpha-server}") String serviceId,
            @Value("${alpha.cluster.ssl.enable:false}") boolean enableSSL,
            @Value("${alpha.cluster.ssl.mutualAuth:false}") boolean mutualAuth,
            @Value("${alpha.cluster.ssl.cert:client.crt}") String cert,
            @Value("${alpha.cluster.ssl.key:client.pem}") String key,
            @Value("${alpha.cluster.ssl.certChain:ca.crt}") String certChain,
            @Lazy MessageHandler handler,
            @Lazy TccMessageHandler tccMessageHandler) {

        List<String> eurekaServiceUrls = new ArrayList<>();
        String[] zones = eurekaClientConfig.getAvailabilityZones(eurekaClientConfig.getRegion());
        for (String zone : zones) {
            eurekaServiceUrls.addAll(eurekaClientConfig.getEurekaServerServiceUrls(zone));
        }
        LOG.info("alpha cluster eureka config enabled, eureka server {}", String.join(",", eurekaServiceUrls));
        String[] alphaAddresses = this.getAlphaAddress(serviceId);
        if (alphaAddresses.length > 0) {
            if (addresses != null && addresses.length > 0) {
                LOG.warn("get alpha cluster address {} from eureka server, ignore alpha.cluster.address={}", String.join(",", alphaAddresses), String.join(",", addresses));
            } else {
                LOG.warn("get alpha cluster address {} from eureka server, ", String.join(",", alphaAddresses));
            }
            addresses = alphaAddresses;
        } else {
            LOG.warn("could not find alpha cluster address from eureka server, use default address {}", String.join(",", addresses));
        }

        MessageFormat messageFormat = new KryoMessageFormat();
        AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
                .addresses(ImmutableList.copyOf(addresses))
                .enableSSL(enableSSL)
                .enableMutualAuth(mutualAuth)
                .cert(cert)
                .key(key)
                .certChain(certChain)
                .messageDeserializer(messageFormat)
                .messageSerializer(messageFormat)
                .messageHandler(handler)
                .tccMessageHandler(tccMessageHandler)
                .build();
        return clusterConfig;
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
                LOG.warn("Alpha has been found in Eureka, but Alpha's registered address information is not found in metadata. Please check Alpha is configured spring.profiles.active=spring-cloud");
            }
        } else {
            LOG.warn("No Alpha Server {} found in the Eureka", serviceId);
        }
        return alphaAddresses.toArray(new String[alphaAddresses.size()]);
    }
}
