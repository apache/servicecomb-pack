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

import org.apache.servicecomb.pack.alpha.server.AlphaApplication;
import org.apache.servicecomb.pack.alpha.server.AlphaConfig;
import org.apache.servicecomb.pack.alpha.server.cluster.master.provider.jdbc.jpa.MasterLock;
import org.apache.servicecomb.pack.alpha.server.cluster.master.provider.jdbc.jpa.MasterLockRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
        properties = {
                "alpha.cluster.master.enabled=true",
                "alpha.server.host=0.0.0.0",
                "alpha.server.port=8090",
                "alpha.event.pollingInterval=1",
                "spring.main.allow-bean-definition-overriding=true"
        })
public class ClusterLockServiceTest {

    @Value("[${alpha.server.host}]:${alpha.server.port}")
    private String instanceId;

    @Value("${spring.application.name:servicecomb-alpha-server}")
    private String serviceName;

    @Value("${alpha.cluster.master.expire:5000}")
    private int expire;

    @Autowired
    ClusterLockService clusterLockService;

    @MockBean
    private MasterLockRepository masterLockRepository;

    private final MasterLock masterLock = populateMasterLock(serviceName, instanceId, expire);


    @Test(timeout = 5000)
    public void testIsMaster() {
        when(masterLockRepository.initLock(masterLock)).thenReturn(Boolean.TRUE);
        when(masterLockRepository.findMasterLockByServiceName(serviceName)).thenReturn(Optional.of(masterLock));
        when(masterLockRepository.updateLock(masterLock)).thenReturn(Boolean.TRUE);
        while(clusterLockService.isLockExecuted()) {
            Assert.assertEquals(clusterLockService.isMasterNode(), Boolean.TRUE);
        }
    }

    @Test(timeout = 5000)
    public void testIsSlave() {
        when(masterLockRepository.initLock(masterLock)).thenReturn(Boolean.FALSE);
        when(masterLockRepository.findMasterLockByServiceName(serviceName)).thenReturn(Optional.of(masterLock));
        when(masterLockRepository.updateLock(masterLock)).thenReturn(Boolean.FALSE);
        while(clusterLockService.isLockExecuted()) {
            Assert.assertEquals(clusterLockService.isMasterNode(), Boolean.FALSE);
        }
    }

    private MasterLock populateMasterLock(String serviceName, String instanceId, int expire) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        Date lockedTime = cal.getTime();
        cal.add(Calendar.MILLISECOND, expire);
        Date expireTime = cal.getTime();
        return new MasterLock(serviceName, instanceId, expireTime, lockedTime);
    }
}
