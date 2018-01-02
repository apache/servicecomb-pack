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

package org.apache.servicecomb.saga.alpha.core;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

public class PendingTaskRunnerTest {
  private final List<String> messages = new ArrayList<>();
  private final BlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();
  private final PendingTaskRunner taskRunner = new PendingTaskRunner(runnables, 10);

  @Test
  public void burnsAllTasksInQueue() throws Exception {
    runnables.offer(() -> messages.add("hello"));
    runnables.offer(() -> messages.add("world"));

    taskRunner.run();

    await().atMost(500, MILLISECONDS).until(runnables::isEmpty);

    assertThat(messages, contains("hello", "world"));
  }

  @Test
  public void exitOnInterruption() throws Exception {
    taskRunner.run().cancel(true);

    runnables.offer(() -> messages.add("hello"));
    Thread.sleep(300);

    assertThat(runnables.isEmpty(), is(false));
    assertThat(messages.isEmpty(), is(true));
  }
}
