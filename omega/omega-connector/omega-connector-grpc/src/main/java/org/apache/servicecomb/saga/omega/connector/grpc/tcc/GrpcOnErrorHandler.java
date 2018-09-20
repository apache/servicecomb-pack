/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.omega.connector.grpc.tcc;

import com.google.common.base.Supplier;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.servicecomb.saga.omega.connector.grpc.PushBackReconnectRunnable;
import org.apache.servicecomb.saga.omega.connector.grpc.RetryableMessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

public class GrpcOnErrorHandler {

  private final BlockingQueue<Runnable> pendingTasks;

  private final Map<MessageSender, Long> senders;

  private final GrpcRetryContext grpcRetryContext;

  public GrpcOnErrorHandler(BlockingQueue<Runnable> pendingTasks, Map<MessageSender, Long> senders) {
    this.pendingTasks = pendingTasks;
    this.senders = senders;
    this.grpcRetryContext = new GrpcRetryContext();
  }

  public void handle(MessageSender messageSender) {
    final Runnable runnable = new PushBackReconnectRunnable(
        messageSender,
        senders,
        pendingTasks,
        grpcRetryContext.getReconnectedSenders()
    );
    synchronized (pendingTasks) {
      if (!pendingTasks.contains(runnable)) {
        pendingTasks.offer(runnable);
      }
    }
  }

  public GrpcRetryContext getGrpcRetryContext() {
    return grpcRetryContext;
  }

  public static class GrpcRetryContext {

    private final BlockingQueue<MessageSender> reconnectedSenders = new LinkedBlockingQueue<>();

    private final MessageSender retryMessageSender = new RetryableMessageSender(reconnectedSenders);

    private final Supplier<MessageSender> defaultMessageSender = new Supplier<MessageSender>() {
      @Override
      public MessageSender get() {
        return retryMessageSender;
      }
    };

    public BlockingQueue<MessageSender> getReconnectedSenders() {
      return reconnectedSenders;
    }

    public Supplier<MessageSender> getDefaultMessageSender() {
      return defaultMessageSender;
    }
  }
}
