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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TccEventScanner {

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private final TccTxEventService tccTxEventService;

  private final int delay;

  private final long globalTxTimeoutSeconds;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TccEventScanner(TccTxEventService tccTxEventService, int delay, long globalTxTimeoutSeconds) {
    this.tccTxEventService = tccTxEventService;
    this.delay = delay;
    this.globalTxTimeoutSeconds = globalTxTimeoutSeconds;
  }

  public void start() {
    scheduler.scheduleWithFixedDelay(() -> {
      tccTxEventService.handleTimeoutTx(new Date(System.currentTimeMillis() - SECONDS.toMillis(globalTxTimeoutSeconds)), 1);
      tccTxEventService.clearCompletedGlobalTx(1);
    }, 0, delay, MILLISECONDS);
  }

  public void shutdown() {
    scheduler.shutdown();
  }
}
