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

import com.google.common.base.Optional;
import io.grpc.ManagedChannel;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.saga.omega.connector.grpc.MessageSenderPicker;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoadBalanceSenderAdapter implements MessageSender {

  private final LoadBalanceContext loadContext;

  private final MessageSenderPicker senderPicker;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public LoadBalanceSenderAdapter(
      LoadBalanceContext loadContext,
      MessageSenderPicker senderPicker) {
    this.loadContext = loadContext;
    this.senderPicker = senderPicker;
  }

  @SuppressWarnings("unchecked")
  public <T> T pickMessageSender() {
    return (T) senderPicker.pick(loadContext.getSenders(),
        loadContext.getGrpcOnErrorHandler().getGrpcRetryContext().getDefaultMessageSender());
  }

  public <T> Optional<AlphaResponse> doGrpcSend(MessageSender messageSender, T event, SenderExecutor<T> executor) {
    AlphaResponse response = null;
    try {
      long startTime = System.nanoTime();
      response = executor.apply(event);
      loadContext.getSenders().put(messageSender, System.nanoTime() - startTime);
    } catch (OmegaException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Retry sending event {} due to failure", event, e);
      loadContext.getSenders().put(messageSender, Long.MAX_VALUE);
//      loadContext.getGrpcOnErrorHandler().handle(messageSender);
    }
    return Optional.fromNullable(response);
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
    loadContext.getPendingTaskRunner().shutdown();
    for(ManagedChannel channel : loadContext.getChannels()) {
      channel.shutdownNow();
    }
  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    return null;
  }

  public MessageSenderPicker getSenderPicker() {
    return senderPicker;
  }
}
