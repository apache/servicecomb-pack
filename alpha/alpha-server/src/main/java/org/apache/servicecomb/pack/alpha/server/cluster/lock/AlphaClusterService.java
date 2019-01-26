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

package org.apache.servicecomb.pack.alpha.server.cluster.lock;

import org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.LockProvider;
import org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.MasterLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "alpha.cluster.enabled", havingValue = "true")
@EnableScheduling
public class AlphaClusterService {

    private static final Logger LOG = LoggerFactory.getLogger(AlphaClusterService.class);


    public AlphaClusterService(){
        LOG.info("Initialize distributed mode");
    }

    public boolean isMaser() {
        return LockConfiguration.isMaster;
    }


    @Autowired
    LockProvider lockProvider;

    @Scheduled(fixedRate = 10000)
    public void masterfixedRate() {
        LockConfiguration lockConfig = new LockConfiguration("alpha-server", Instant.now());
        Optional<MasterLock> lock = lockProvider.lock(lockConfig);
        if (lock.isPresent()) {
            if (!LockConfiguration.isMaster) {
                LockConfiguration.isMaster = Boolean.TRUE;
                LOG.info("I am master");
            }
        } else {
            if (LockConfiguration.isMaster) {
                LOG.info("I am slave");
                LockConfiguration.isMaster = Boolean.FALSE;
            }
            LOG.debug("Not executing {}. It's locked.", lockConfig.getName());
        }
    }


}
