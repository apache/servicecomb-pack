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

package org.apache.servicecomb.pack.alpha.server.cluster.lock.provider;


import org.apache.servicecomb.pack.alpha.server.cluster.lock.LockConfiguration;

import java.util.Optional;

/**
 * 分布式抢占抽象类
 * 基于id和locked_until的数据集，可以是表也可以是集合
 * 尝试写入一条投票记录，同时在内存中保存这条记录，如果这条记录存在，那么认为投票成功
 * 我们将找到ID==name的这条记录，更新这条记录的lock_until为当前时间
 * 如果返回更新1条记录的信息，那么则投票成功。如果返回0条更新的信息，则投票失败
 * 当弃权时，lock_until被设置成当前时间
 */
public abstract class AbstractLockProvider implements LockProvider {
    private final LockProviderPersistence lockProviderPersistence;
    private final LockRecordRegistry lockRecordRegistry = new LockRecordRegistry();

    protected AbstractLockProvider(LockProviderPersistence lockProviderPersistence) {
        this.lockProviderPersistence = lockProviderPersistence;
    }

    @Override
    public Optional<MasterLock> lock(LockConfiguration lockConfiguration) {
        boolean lockObtained = doLock(lockConfiguration);
        if (lockObtained) {
            return Optional.of(new Locked(lockConfiguration, lockProviderPersistence));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 如果lockUntil等于当前时间，则根据LockConfiguration设置lockUntil
     */
    protected boolean doLock(LockConfiguration lockConfiguration) {
        String name = lockConfiguration.getName();
        //缓存处理
        if (!lockRecordRegistry.lockRecordRecentlyCreated(name)) {
            // 缓存中不包含时进行投票
            if (lockProviderPersistence.lock(lockConfiguration)) {
                // 投票成功后加入缓存
                lockRecordRegistry.addLockRecord(name);
                return true;
            }
            //缓存重入
            lockRecordRegistry.addLockRecord(name);
        }
        return lockProviderPersistence.relock(lockConfiguration);
    }

    private static class Locked implements MasterLock {
        private final LockConfiguration lockConfiguration;
        private final LockProviderPersistence lockProviderPersistence;

        Locked(LockConfiguration lockConfiguration, LockProviderPersistence lockProviderPersistence) {
            this.lockConfiguration = lockConfiguration;
            this.lockProviderPersistence = lockProviderPersistence;
        }

        @Override
        public void unlock() {
            lockProviderPersistence.unlock(lockConfiguration);
        }
    }

}
