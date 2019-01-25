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

import java.time.Instant;
import java.util.Objects;

/**
 * Election configuration.
 */
public class LockConfiguration {
    private final String name;
    public static boolean isMaster = Boolean.FALSE;

    /**
     * The vote is held until this instant, after that it's automatically released (the process holding it has most likely
     * died without releasing the vote) Can be ignored by providers which can detect dead processes (like Zookeeper)
     */
    private final Instant lockAtMostUntil;
    /**
     * The vote will be held until this time even if the task holding the vote finishes earlier.
     */
    private final Instant lockAtLeastUntil;    

    public LockConfiguration(String name, Instant lockUntil) {
        this(name, lockUntil, Instant.now());
    }

    public LockConfiguration(String name, Instant lockUntil, Instant lockAtLeastUntil) {
        this.name = Objects.requireNonNull(name);
        this.lockAtMostUntil = Objects.requireNonNull(lockUntil);
        this.lockAtLeastUntil = Objects.requireNonNull(lockAtLeastUntil);
        if (lockAtLeastUntil.isAfter(lockAtMostUntil)) {
            throw new IllegalArgumentException("lockAtMost is before lockAtLeast for vote '" + name + "'.");
        }
        if (lockAtMostUntil.isBefore(Instant.now())) {
            throw new IllegalArgumentException("lockAtMost is in the past for vote '" + name + "'.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("vote name can not be empty");
        }
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public Instant getLockUntil() {
        return lockAtMostUntil;
    }

    public Instant getLockAtMostUntil() {
        return lockAtMostUntil;
    }

    public Instant getLockAtLeastUntil() {
        return lockAtLeastUntil;
    }

    /**
     * Returns either now or lockAtLeastUntil whichever is later.
     */
    public Instant getUnlockTime() {
        Instant now = Instant.now();
        return lockAtLeastUntil.isAfter(now) ? lockAtLeastUntil : now;
    }

    @Override
    public String toString() {
        return "LockConfiguration{" +
            "name='" + name + '\'' +
            ", lockAtMostUntil=" + lockAtMostUntil +
            ", lockAtLeastUntil=" + lockAtLeastUntil +
            '}';
    }
}
