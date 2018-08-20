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

package org.apache.servicecomb.saga.core;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SagaTaskFactory {
  private final FallbackPolicy fallbackPolicy;
  private final RetrySagaLog retrySagaLog;
  private final PersistentStore persistentStore;

  public SagaTaskFactory(int retryDelay, PersistentStore persistentStore) {
    this.persistentStore = persistentStore;

    fallbackPolicy = new FallbackPolicy(retryDelay);
    retrySagaLog = new RetrySagaLog(persistentStore, retryDelay);
  }

  public Map<String, SagaTask> sagaTasks(final String sagaId,
      final String requestJson,
      final RecoveryPolicy recoveryPolicy,
      final EventStore sagaLog) {

    final SagaLog compositeSagaLog = compositeSagaLog(sagaLog, persistentStore);

    return new HashMap<String, SagaTask>() {{
      put(SagaTask.SAGA_START_TASK, new SagaStartTask(sagaId, requestJson, compositeSagaLog));

      SagaLog retrySagaLog = compositeSagaLog(sagaLog, SagaTaskFactory.this.retrySagaLog);
      put(SagaTask.SAGA_REQUEST_TASK,
          new RequestProcessTask(sagaId, retrySagaLog, new LoggingRecoveryPolicy(recoveryPolicy), fallbackPolicy));
      put(SagaTask.SAGA_END_TASK, new SagaEndTask(sagaId, retrySagaLog));
    }};
  }

  private CompositeSagaLog compositeSagaLog(SagaLog sagaLog, PersistentLog persistentLog) {
    return new CompositeSagaLog(sagaLog, persistentLog);
  }

  static class RetrySagaLog implements PersistentLog {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PersistentStore persistentStore;
    private final int retryDelay;

    RetrySagaLog(PersistentStore persistentStore, int retryDelay) {
      this.persistentStore = persistentStore;
      this.retryDelay = retryDelay;
    }

    @Override
    public void offer(SagaEvent sagaEvent) {
      boolean success = false;
      do {
        try {
          persistentStore.offer(sagaEvent);
          success = true;
          log.info("Persisted saga event {} successfully", sagaEvent);
        } catch (Exception e) {
          log.error("Failed to persist saga event {}", sagaEvent, e);
          sleep(retryDelay);
        }
      } while (!success && !isInterrupted());
    }

    private boolean isInterrupted() {
      return Thread.currentThread().isInterrupted();
    }

    private void sleep(int delay) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
