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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.callback;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TccPendingTaskRunner {

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private final BlockingQueue<Runnable> pendingTasks = new LinkedBlockingQueue<>();

  private final int delay;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TccPendingTaskRunner(int delay) {
    this.delay = delay;
  }

  public void start() {
    scheduler.scheduleWithFixedDelay(() -> {
      try {
        pendingTasks.take().run();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
    }, 0, delay, MILLISECONDS);
  }

  public void shutdown() {
    scheduler.shutdown();
  }

  public BlockingQueue<Runnable> getPendingTasks() {
    return pendingTasks;
  }
}
