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
 * Activate @EnableEurekaClient with the properties spring.profiles.active: spring-cloud-eurek
 */

package org.apache.servicecomb.pack.alpha.server.extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

@Configuration
@Profile(value= {"spring-cloud-eureka"})
@EnableEurekaClient
public class SpringCloudEurekaConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(SpringCloudEurekaConfiguration.class);

    @PostConstruct
    public void init(){
        LOG.info("registering to spring cloud eureka enabled");
    }
}
