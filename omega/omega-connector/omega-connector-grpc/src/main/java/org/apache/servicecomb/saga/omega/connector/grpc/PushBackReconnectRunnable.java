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

package org.apache.servicecomb.saga.omega.connector.grpc;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PushBackReconnectRunnable implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MessageSender messageSender;
  private final Map<MessageSender, Long> senders;
  private final BlockingQueue<Runnable> pendingTasks;

  private final BlockingQueue<MessageSender> connectedSenders;

  PushBackReconnectRunnable(
      MessageSender messageSender,
      Map<MessageSender, Long> senders,
      BlockingQueue<Runnable> pendingTasks,
      BlockingQueue<MessageSender> connectedSenders) {
    this.messageSender = messageSender;
    this.senders = senders;
    this.pendingTasks = pendingTasks;
    this.connectedSenders = connectedSenders;
  }

  @Override
  public void run() {
    try {
      LOG.info("Retry connecting to alpha at {}", messageSender.target());
      messageSender.onDisconnected();
      messageSender.onConnected();
      senders.put(messageSender, 0L);
      connectedSenders.offer(messageSender);
      LOG.info("Retry connecting to alpha at {} is successful", messageSender.target());
    } catch (Exception e) {
      LOG.error("Failed to reconnect to alpha at {}", messageSender.target(), e);
      pendingTasks.offer(this);
    }
  }
}
