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

import org.apache.servicecomb.pack.alpha.core.Election;
import org.apache.servicecomb.pack.alpha.core.ElectionRepository;
import org.apache.servicecomb.pack.alpha.server.cluster.lock.LockConfiguration;
import org.apache.servicecomb.pack.alpha.server.cluster.lock.provider.LockProviderPersistence;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * 基于数据库的投票持久化接口
 */
class JdbcLockLockProviderPersistence implements LockProviderPersistence {

    private final ElectionRepository electionRepository;

    JdbcLockLockProviderPersistence(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public boolean lock(LockConfiguration lockConfiguration) {
        Election election = new Election(lockConfiguration.getName(),
                Timestamp.from(lockConfiguration.getLockAtMostUntil()),
                Timestamp.from(Instant.now()),
                this.getHostname());
        return this.electionRepository.vote(election);
    }

    public boolean relock(LockConfiguration lockConfiguration) {
        Election election = new Election(lockConfiguration.getName(),
                Timestamp.from(lockConfiguration.getLockAtMostUntil()),
                Timestamp.from(Instant.now()),
                this.getHostname());
        return this.electionRepository.revote(election);
    }

    public void unlock(LockConfiguration lockConfiguration) {
        this.electionRepository.abstain(lockConfiguration.getName(),
                Timestamp.from(lockConfiguration.getLockAtMostUntil()));
    }

    public String getHostname() {
        return "localhost:8080";
    }


}
