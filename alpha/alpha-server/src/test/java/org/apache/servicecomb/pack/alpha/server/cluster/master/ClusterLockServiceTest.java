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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.apache.servicecomb.pack.alpha.server.cluster.master.provider.jdbc.MasterLockEntityRepository;

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
  private MasterLockEntityRepository masterLockEntityRepository;

  @Autowired
  MasterLockRepository masterLockRepository;


  @Test(timeout = 5000)
  public void testMasterNode() throws InterruptedException {
    MasterLock masterLock = clusterLockService.getMasterLock();
    Assert.assertEquals(masterLock.getServiceName(),serviceName);
    Assert.assertEquals(masterLock.getInstanceId(),instanceId);
    Assert.assertEquals((masterLock.getExpireTime().getTime()-masterLock.getLockedTime().getTime()),expire);
    when(masterLockEntityRepository.initLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(1);
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenReturn(Optional.of(masterLock));
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(1);
    Thread.sleep(2000);
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), true);
    clusterLockService.unLock();
  }

  @Test(timeout = 5000)
  public void testMasterNodeWhenFirst() throws InterruptedException {
    MasterLock masterLock = clusterLockService.getMasterLock();
    Assert.assertEquals(masterLock.getServiceName(),serviceName);
    Assert.assertEquals(masterLock.getInstanceId(),instanceId);
    Assert.assertEquals((masterLock.getExpireTime().getTime()-masterLock.getLockedTime().getTime()),expire);
    when(masterLockEntityRepository.initLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(1);
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenReturn(Optional.empty());
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(1);
    Thread.sleep(2000);
    clusterLockService.unLock();
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), true);
    clusterLockService.unLock();
  }

  @Test(timeout = 5000)
  public void testSlaveNodeWhenDuplicateKey() throws InterruptedException {
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenReturn(Optional.empty());
    when(masterLockEntityRepository.initLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenThrow(new RuntimeException("duplicate key"));
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(0);
    Thread.sleep(2000);
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), false);
    clusterLockService.unLock();
  }

  @Test(timeout = 5000)
  public void testSlaveNodeUpdateLockLater() throws InterruptedException {
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenReturn(Optional.of(clusterLockService.getMasterLock()));
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(0);
    Thread.sleep(2000);
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), false);
    clusterLockService.unLock();
  }

  @Test(timeout = 5000)
  public void testSlaveNodeUpdateLockLaterByFirst() throws InterruptedException {
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenReturn(Optional.of(clusterLockService.getMasterLock()));
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(0);
    Thread.sleep(2000);
    clusterLockService.unLock();
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), false);
    clusterLockService.unLock();
  }

  @Test(timeout = 3000)
  public void testSlaveNodeWhenInitLockException() throws InterruptedException {
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenThrow(new RuntimeException("initLock Exception"));
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(0);
    Thread.sleep(2000);
    clusterLockService.unLock();
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), false);
    clusterLockService.unLock();
  }

  @Test(timeout = 5000)
  public void testSlaveNodeWhenUpdateLockException() throws InterruptedException {
    when(masterLockEntityRepository.initLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenReturn(1);
    when(masterLockEntityRepository.findMasterLockByServiceName(Mockito.anyString())).thenReturn(Optional.empty());
    when(masterLockEntityRepository.updateLock(Mockito.anyString(),Mockito.any(Date.class),Mockito.any(Date.class),Mockito.anyString())).thenThrow(new RuntimeException("updateLock Exception"));
    Thread.sleep(2000);
    clusterLockService.unLock();
    while(!clusterLockService.isLockExecuted()) {
      Thread.sleep(50);
    }
    Assert.assertEquals(clusterLockService.isMasterNode(), false);
    clusterLockService.unLock();
  }
}
