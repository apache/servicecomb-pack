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

package org.apache.servicecomb.pack.alpha.spec.saga.db.cluster;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PostConstruct;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.event.GrpcStartableStartedEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.Lock;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.LockProvider;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cluster master preemption master service
 * default based on database master_lock table implementation
 * <p>
 * Set true to enable default value false
 * alpha.spec.saga.db.cluster=true
 * <p>
 * Implementation type, default jdbc
 * alpha.spec.saga.db.cluster.type=jdbc
 * <p>
 * Lock timeout, default value 5000 millisecond
 * alpha.spec.saga.db.cluster.expire=5000
 */

public class ClusterLockService implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean locked;

  private boolean lockExecuted;

  private boolean applicationReady;

  private boolean portReady;

  private MasterLock masterLock;

  private Optional<Lock> locker;

  private Timer masterCheckTimer;

  @Value("[${alpha.server.host}]:${alpha.server.port}")
  private String instanceId;

  @Value("${spring.application.name:servicecomb-alpha-server}")
  private String serviceName;

  @Value("${alpha.spec.saga.db.cluster.expire:5000}")
  private int expire;

  @Autowired
  LockProvider lockProvider;

  @Autowired
  NodeStatus nodeStatus;

  @Autowired
  @Qualifier("alphaEventBus")
  EventBus eventBus;

  @PostConstruct
  public void init() {
    eventBus.register(this);

    /**
     * Try to lock every second
     * TODO We need to check if the master check interval time check is OK
     * */
    masterCheckTimer = new Timer("masterCheckTimer");
    masterCheckTimer.schedule(new TimerTask(){

      @Override
      public void run() {
        if (applicationReady && portReady) {
          locker = lockProvider.lock(getMasterLock());
          if (locker.isPresent()) {
            if (!locked) {
              locked = true;
              nodeStatus.setTypeEnum(NodeStatus.TypeEnum.MASTER);
              LOG.info("Master Node");
            }
            //Keep locked
          } else {
            if (locked || !lockExecuted) {
              locked = false;
              nodeStatus.setTypeEnum(NodeStatus.TypeEnum.SLAVE);
              LOG.info("Slave Node");
            }
          }
          lockExecuted = true;
        }
      }
    },0, 1000);

    LOG.info("Initialize cluster mode");
  }

  public boolean isMasterNode() {
    return locked;
  }

  public boolean isLockExecuted() {
    return lockExecuted;
  }

  public MasterLock getMasterLock() {
    if (this.masterLock == null) {
      this.masterLock = new MasterLock(serviceName, instanceId);
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    this.masterLock.setLockedTime(cal.getTime());
    cal.add(Calendar.MILLISECOND, expire);
    this.masterLock.setExpireTime(cal.getTime());
    return this.masterLock;
  }

  public void unLock() {
    if (this.locker.isPresent()) {
      this.locker.get().unlock();
    }
    lockExecuted = false;
    locked = false;
    nodeStatus.setTypeEnum(NodeStatus.TypeEnum.SLAVE);
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
    this.applicationReady = true;
    if(this.instanceId!=null && this.instanceId.endsWith(":0")){
      this.portReady = false;
    }else{
      this.portReady = true;
    }
  }

  /**
   * Update the port number in the instance ID when using a random gRPC port
   * */
  @Subscribe
  public void listenGrpcStartableStartedEvent(GrpcStartableStartedEvent grpcStartableStartedEvent) {
    if(this.instanceId!=null && this.instanceId.endsWith(":0")){
      instanceId = instanceId.replace(":0",":"+grpcStartableStartedEvent.getPort());
      this.portReady = true;
    }
  }
}
