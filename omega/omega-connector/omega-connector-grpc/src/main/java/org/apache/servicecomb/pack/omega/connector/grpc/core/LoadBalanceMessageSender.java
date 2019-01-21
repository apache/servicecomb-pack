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

import io.grpc.ManagedChannel;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoadBalanceMessageSender implements MessageSender {

  private final LoadBalanceContext loadContext;

  private final MessageSenderPicker senderPicker;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public LoadBalanceMessageSender(
      LoadBalanceContext loadContext,
      MessageSenderPicker senderPicker) {
    this.loadContext = loadContext;
    this.senderPicker = senderPicker;
  }

  @Override
  public AlphaResponse send(final Object event) {
    do {
      AlphaResponse response = doSend(event);
      if (null != response) {
        return response;
      }
    } while (!Thread.currentThread().isInterrupted());
    throw new OmegaException("Failed to send event " + event + " due to interruption");
  }

  private AlphaResponse doSend(final Object event) {
    AlphaResponse result = null;
    MessageSender messageSender = pickMessageSender();
    try {
      long startTime = System.nanoTime();
      result = messageSender.send(event);
      loadContext.getSenders().put(messageSender, System.nanoTime() - startTime);
    } catch (OmegaException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Retry sending event {} due to failure", event, e);
      loadContext.getSenders().put(messageSender, Long.MAX_VALUE);
    }
    return result;
  }

  public MessageSender pickMessageSender() {
    MessageSender result = senderPicker.pick(loadContext.getSenders());
    return null == result ? ErrorHandleEngineManager.getEngine().getReconnectedSender() : result;
  }

  @Override
  public void onConnected() {
    for(MessageSender sender : loadContext.getSenders().keySet()){
      try {
        sender.onConnected();
      } catch (Exception e) {
        LOG.error("Failed connecting to alpha at {}", sender.target(), e);
      }
    }
  }

  @Override
  public void onDisconnected() {
    for (MessageSender sender : loadContext.getSenders().keySet()) {
      try {
        sender.onDisconnected();
      } catch (Exception e) {
        LOG.error("Failed disconnecting from alpha at {}", sender.target(), e);
      }
    }
  }

  @Override
  public void close() {
    for(ManagedChannel channel : loadContext.getChannels()) {
      channel.shutdownNow();
    }
    ErrorHandleEngineManager.shutdownEngine();
  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  public LoadBalanceContext getLoadContext() {
    return loadContext;
  }
}
