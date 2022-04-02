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

package org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider;

import java.util.Optional;
import org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.jdbc.jpa.MasterLock;

public abstract class AbstractLockProvider implements LockProvider {

  private final LockProviderPersistence lockProviderPersistence;

  private boolean lockInitialization;

  protected AbstractLockProvider(LockProviderPersistence lockProviderPersistence) {
    this.lockProviderPersistence = lockProviderPersistence;
  }

  @Override
  public Optional<org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.Lock> lock(
      MasterLock masterLock) {
    boolean lockObtained = doLock(masterLock);
    if (lockObtained) {
      return Optional.of(new Lock(this,masterLock, lockProviderPersistence));
    } else {
      return Optional.empty();
    }
  }

  protected boolean doLock(MasterLock masterLock) {
    if (!lockInitialization) {
      lockInitialization = true;
      if (lockProviderPersistence.initLock(masterLock)) {
        return true;
      }
    }
    return lockProviderPersistence.updateLock(masterLock);
  }

  public void doUnLock(){
    this.lockInitialization = false;
  }

  private static class Lock implements
      org.apache.servicecomb.pack.alpha.spec.saga.db.cluster.provider.Lock {
    private final MasterLock masterLock;
    private final AbstractLockProvider provider;
    private final LockProviderPersistence lockProviderPersistence;

    Lock(AbstractLockProvider provider, MasterLock masterLock, LockProviderPersistence lockProviderPersistence) {
      this.provider = provider;
      this.masterLock = masterLock;
      this.lockProviderPersistence = lockProviderPersistence;
    }

    @Override
    public void unlock() {
      lockProviderPersistence.unLock(masterLock);
      provider.doUnLock();;
    }
  }
}
