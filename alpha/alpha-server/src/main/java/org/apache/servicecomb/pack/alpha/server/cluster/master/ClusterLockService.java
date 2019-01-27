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

package org.apache.servicecomb.pack.alpha.server.cluster.master;

import org.apache.servicecomb.pack.alpha.server.cluster.master.provider.LockProvider;
import org.apache.servicecomb.pack.alpha.server.cluster.master.provider.Locked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 *
 * Cluster master preemption master service
 * default based on database master_lock table implementation
 *
 * Set true to enable default value false
 * alpha.cluster.master.enabled=true
 *
 * Implementation type, default jdbc
 * alpha.cluster.master.type=jdbc
 *
 * Lock timeout, default value 5000 millisecond
 * alpha.cluster.master.expire=5000
 *
 */

@Component
@ConditionalOnProperty(name = "alpha.cluster.master.enabled", havingValue = "true")
@EnableScheduling
public class ClusterLockService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterLockService.class);
    private boolean locked = Boolean.FALSE;

    @Value("[${alpha.server.host}]:${alpha.server.port}")
    private String instanceId;

    @Value("${spring.application.name:servicecomb-alpha-server}")
    private String serviceName;

    @Value("${alpha.cluster.master.expire:5000}")
    private int expire;

    @Autowired
    LockProvider lockProvider;

    public ClusterLockService() {
        LOG.info("Initialize cluster mode");
    }

    public boolean isLocked() {
        return locked;
    }

    @Scheduled(fixedDelayString = "${alpha.cluster.master.expire:5000}")
    public void masterLock() {
        LockConfig lockConfig = new LockConfig(serviceName, instanceId, expire);
        Optional<Locked> lock = lockProvider.lock(lockConfig);
        if (lock.isPresent()) {
            if (!this.locked) {
                locked = Boolean.TRUE;
                LOG.info("Master Node");
            } else {
                LOG.debug("Keep locked");
            }
        } else {
            locked = Boolean.FALSE;
            LOG.info("Slave Node");
        }
    }

    @Scheduled(fixedRate = 5000)
    public void test() {
        if (this.locked) {
            System.out.println("working...");
        }
    }
}
