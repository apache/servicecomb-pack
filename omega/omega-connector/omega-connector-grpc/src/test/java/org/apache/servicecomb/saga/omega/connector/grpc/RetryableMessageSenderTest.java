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

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.SagaStartedEvent;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.omega.transaction.TxStartedEvent;
import org.junit.Test;

public class RetryableMessageSenderTest {
  @SuppressWarnings("unchecked")
  private final BlockingQueue<MessageSender> availableMessageSenders = new LinkedBlockingQueue<>();
  private final MessageSender messageSender = new RetryableMessageSender(availableMessageSenders);

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");

  private final TxStartedEvent event = new TxStartedEvent(globalTxId, localTxId, null, "method x", 0, null, 0);

  @Test
  public void sendEventWhenSenderIsAvailable() {
    MessageSender sender = mock(MessageSender.class);
    availableMessageSenders.add(sender);

    messageSender.send(event);

    verify(sender, times(1)).send(event);
  }

  @Test
  public void blowsUpWhenEventIsSagaStarted() {
    TxEvent event = new SagaStartedEvent(globalTxId, localTxId, 0);

    try {
      messageSender.send(event);
      expectFailing(OmegaException.class);
    } catch (OmegaException e) {
      assertThat(e.getMessage(),
          is("Failed to process subsequent requests because no alpha server is available"));
    }
  }

  @Test
  public void blowsUpWhenInterrupted() throws InterruptedException {
    Thread thread = new Thread(() -> {
      try {
        messageSender.send(event);
        expectFailing(OmegaException.class);
      } catch (OmegaException e) {
        assertThat(e.getMessage().endsWith("interruption"), is(true));
      }
    });

    thread.start();
    thread.interrupt();
    thread.join();
  }
}