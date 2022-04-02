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

package org.apache.servicecomb.pack.alpha.spec.saga.db;

import org.apache.servicecomb.pack.alpha.server.AlphaApplication;
import org.apache.servicecomb.pack.alpha.server.AlphaConfig;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.ClusterLockService;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.MasterLockEntityRepository;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLock;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLockRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class, AlphaConfig.class},
    properties = {
        "alpha.spec.saga.db.cluster.enabled=true",
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8090",
        "alpha.event.pollingInterval=1",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.profiles.active=test"
    })
public class ClusterLockServiceTest {

  @Value("[${alpha.server.host}]:${alpha.server.port}")
  private String instanceId;

  @Value("${spring.application.name:servicecomb-alpha-server}")
  private String serviceName;

  @Value("${alpha.spec.saga.db.cluster.expire:5000}")
  private int expire;

  @Autowired
  ClusterLockService clusterLockService;

  @MockBean
  private MasterLockEntityRepository masterLockEntityRepository;

  @Autowired
  MasterLockRepository masterLockRepository;

  @Before
  // As clusterLockService stop check if the cluster is locked,
  // In this way we need to clean up the locker for each unit test
  public void before(){
    await().atMost(2, SECONDS).until(() -> {
      if(clusterLockService.isLockExecuted() == true){
        clusterLockService.unLock();
        return true;
      }else{
        return false;
      }
    });
  }

  @Test
  public void testMasterNode() {
    MasterLock masterLock = clusterLockService.getMasterLock();
    Assert.assertEquals(masterLock.getServiceName(),serviceName);
    Assert.assertEquals(masterLock.getInstanceId(),instanceId);
    Assert.assertEquals((masterLock.getExpireTime().getTime()-masterLock.getLockedTime().getTime()),expire);
    when(masterLockEntityRepository.initLock(any(), any(), any(),any())).thenReturn(1);
    when(masterLockEntityRepository.findMasterLockByServiceName(any())).thenReturn(Optional.of(masterLock));
    when(masterLockEntityRepository.updateLock(any(), any(), any(),any())).thenReturn(1);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isLockExecuted() == true);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isMasterNode() == true);
  }

  @Test
  public void testSlaveNodeWhenDuplicateKey() {
    when(masterLockEntityRepository.findMasterLockByServiceName(any())).thenReturn(Optional.empty());
    when(masterLockEntityRepository.initLock(any(), any(), any(),any())).thenThrow(new RuntimeException("duplicate key"));
    when(masterLockEntityRepository.updateLock(any(), any(), any(),any())).thenReturn(0);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isLockExecuted() == true);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isMasterNode() == false);
  }

  @Test
  public void testSlaveNodeUpdateLockLater() {
    when(masterLockEntityRepository.findMasterLockByServiceName(any())).thenReturn(Optional.of(clusterLockService.getMasterLock()));
    when(masterLockEntityRepository.updateLock(any(), any(), any(),any())).thenReturn(0);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isLockExecuted() == true);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isMasterNode() == false);
  }

  @Test
  public void testSlaveNodeWhenInitLockException() {
    when(masterLockEntityRepository.findMasterLockByServiceName(any())).thenThrow(new RuntimeException("initLock Exception"));
    when(masterLockEntityRepository.updateLock(any(), any(), any(),any())).thenReturn(0);
    await().atMost(2, SECONDS).until(() -> clusterLockService.isLockExecuted() == true);
    clusterLockService.unLock();
    await().atMost(2, SECONDS).until(() -> clusterLockService.isMasterNode() == false);
  }

  @Test
  public void testSlaveNodeWhenUpdateLockException() {
    when(masterLockEntityRepository.initLock(any(), any(), any(),any())).thenReturn(1);
    when(masterLockEntityRepository.findMasterLockByServiceName(any())).thenReturn(Optional.of(clusterLockService.getMasterLock()));
    when(masterLockEntityRepository.updateLock(any(), any(), any(),any())).thenThrow(new RuntimeException("updateLock Exception"));
    await().atMost(2, SECONDS).until(() -> clusterLockService.isLockExecuted() == true);
    clusterLockService.unLock();
    await().atMost(2, SECONDS).until(() -> clusterLockService.isMasterNode() == false);
  }
}
