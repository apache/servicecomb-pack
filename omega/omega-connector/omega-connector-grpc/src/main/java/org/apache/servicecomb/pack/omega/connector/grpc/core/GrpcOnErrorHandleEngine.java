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

package org.apache.servicecomb.pack.omega.connector.grpc.core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;

public class GrpcOnErrorHandleEngine implements AutoCloseable {

  private final PendingTaskRunner pendingTaskRunner;

  private final Map<MessageSender, Long> senders;

  private final long timeoutSeconds;

  private final BlockingQueue<Runnable> pendingTasks = new LinkedBlockingQueue<>();

  private final BlockingQueue<MessageSender> reconnectedSenders = new LinkedBlockingQueue<>();

  public GrpcOnErrorHandleEngine(Map<MessageSender, Long> senders, long reconnectDelay, long timeoutSeconds) {
    this.pendingTaskRunner = new PendingTaskRunner(pendingTasks, reconnectDelay);
    this.senders = senders;
    this.timeoutSeconds = timeoutSeconds;
  }

  public void start() {
    pendingTaskRunner.start();
  }

  public PendingTaskRunner getPendingTaskRunner() {
    return pendingTaskRunner;
  }

  public void offerTask(final MessageSender messageSender) {
    final Runnable runnable = new PushBackReconnectRunnable(
        messageSender,
        senders,
        pendingTasks,
        reconnectedSenders
    );
    synchronized (pendingTasks) {
      if (!pendingTasks.contains(runnable)) {
        pendingTasks.offer(runnable);
      }
    }
  }

  public MessageSender getDefaultSender() {
    try {
      MessageSender messageSender = reconnectedSenders.poll(timeoutSeconds, TimeUnit.SECONDS);
      if (null == messageSender) {
        throw new OmegaException("Failed to get reconnected sender, all alpha server is down.");
      }
      return messageSender;
    } catch (InterruptedException e) {
      throw new OmegaException("Failed to get reconnected sender", e);
    }
  }

  @Override
  public void close() {
    pendingTaskRunner.shutdown();
  }
}
