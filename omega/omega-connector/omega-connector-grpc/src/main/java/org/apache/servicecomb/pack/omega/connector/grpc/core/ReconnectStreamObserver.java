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

import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;

import java.util.concurrent.CountDownLatch;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReconnectStreamObserver<T> implements StreamObserver<T> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final LoadBalanceContext loadContext;

  private final MessageSender messageSender;

  private final CountDownLatch latch = new CountDownLatch(1);

  public ReconnectStreamObserver(
      LoadBalanceContext loadContext, MessageSender messageSender) {
    this.loadContext = loadContext;
    this.messageSender = messageSender;
  }

  @Override
  public void onError(Throwable t) {
    LOG.error("Failed to process grpc coordinate command.", t);
    loadContext.getGrpcOnErrorHandler().handle(messageSender);
    cancelWait();
  }

  @Override
  public void onCompleted() {
    // Do nothing here
  }

  public void cancelWait(){
    latch.countDown();
  }

  public void waitConnected() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
