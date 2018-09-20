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

import io.grpc.ManagedChannel;
import java.util.Collection;
import java.util.Map;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

public class LoadBalanceContext {

  private Map<MessageSender, Long> senders;

  private final Collection<ManagedChannel> channels;

  private final PendingTaskRunner pendingTaskRunner;

  private final GrpcOnErrorHandler grpcOnErrorHandler;

  public LoadBalanceContext(Map<MessageSender, Long> senders,
      Collection<ManagedChannel> channels, int reconnectDelay) {
    this.senders = senders;
    this.channels = channels;
    this.pendingTaskRunner = new PendingTaskRunner(reconnectDelay);
    this.grpcOnErrorHandler = new GrpcOnErrorHandler(pendingTaskRunner.getPendingTasks(), senders);
    pendingTaskRunner.start();
  }

  public Map<MessageSender, Long> getSenders() {
    return senders;
  }

  public Collection<ManagedChannel> getChannels() {
    return channels;
  }

  public PendingTaskRunner getPendingTaskRunner() {
    return pendingTaskRunner;
  }

  public GrpcOnErrorHandler getGrpcOnErrorHandler() {
    return grpcOnErrorHandler;
  }

  public void setSenders(Map<MessageSender, Long> senders) {
    this.senders = senders;
  }
}
