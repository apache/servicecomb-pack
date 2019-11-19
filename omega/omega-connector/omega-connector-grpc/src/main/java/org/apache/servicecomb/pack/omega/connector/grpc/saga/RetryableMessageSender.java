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

package org.apache.servicecomb.pack.omega.connector.grpc.saga;

import static org.apache.servicecomb.pack.common.EventType.SagaStartedEvent;

import java.util.concurrent.BlockingQueue;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.TxEvent;

public class RetryableMessageSender implements SagaMessageSender {
  private final BlockingQueue<MessageSender> availableMessageSenders;

  public RetryableMessageSender(BlockingQueue<MessageSender> availableMessageSenders) {
    this.availableMessageSenders = availableMessageSenders;
  }

  @Override
  public void onConnected() {

  }

  @Override
  public void onDisconnected() {

  }

  @Override
  public ServerMeta onGetServerMeta() {
    return null;
  }

  @Override
  public void close() {

  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    if (event.type() == SagaStartedEvent) {
      throw new OmegaException("Failed to process subsequent requests because no alpha server is available");
    }
    try {
      return ((SagaMessageSender)availableMessageSenders.take()).send(event);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OmegaException("Failed to send event " + event + " due to interruption", e);
    }
  }
}
