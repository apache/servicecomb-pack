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

package org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.jdbc;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;
import org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.jdbc.jpa.MasterLock;
import org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.jdbc.jpa.MasterLockRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Date;

@EnableKamon
@ConditionalOnProperty(name = "alpha.cluster.enabled", havingValue = "true")
public class SpringMasterLockRepository implements MasterLockRepository {
    private final MasterLockEntityRepository electionRepo;

    SpringMasterLockRepository(MasterLockEntityRepository electionRepo) {
        this.electionRepo = electionRepo;
    }

    @Override
    @Segment(name = "MasterLockInit", category = "application", library = "kamon")
    public boolean initLock(MasterLock masterLock) {
        //尝试初始化锁，锁已经存在返回true，锁不存在返回false
        try {
            if (!electionRepo.existsById(masterLock.getServiceName())) {
                electionRepo.initLock(masterLock.getServiceName(),
                        masterLock.getExpireTime(),
                        masterLock.getLockedTime(),
                        masterLock.getInstanceId());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Segment(name = "MasterUpdateLock", category = "application", library = "kamon")
    public boolean updateLock(MasterLock masterLock) {
        try {
            int size = electionRepo.updateLock(
                    masterLock.getServiceName(),
                    new Date(),
                    masterLock.getExpireTime(),
                    masterLock.getInstanceId());
            return size > 0 ? true : false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Segment(name = "MasterUnLock", category = "application", library = "kamon")
    public void unLock(String serviceName, Date expireTime) {
        electionRepo.unLock(serviceName, expireTime);
    }
}
