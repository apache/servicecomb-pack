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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class PushBackReconnectRunnableTest {
  private static final Runnable NO_OP_RUNNABLE = new Runnable() {
    @Override
    public void run() {
      // Do nothing here
    }
  };

  private final MessageSender sender = mock(MessageSender.class);
  private final BlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();
  private final BlockingQueue<MessageSender> connectedSenders = new LinkedBlockingQueue<>();
  private final Map<MessageSender, Long> senders = new HashMap<>();

  private final PushBackReconnectRunnable pushBack = new PushBackReconnectRunnable(sender, senders, runnables, connectedSenders);

  @Before
  public void setUp() throws Exception {
    runnables.offer(NO_OP_RUNNABLE);
    senders.put(sender, Long.MAX_VALUE);
  }

  @Test
  public void pushFailedCallbackToEndOfQueue() throws Exception {
    doThrow(RuntimeException.class).doThrow(RuntimeException.class).doNothing().when(sender).onDisconnected();
    assertThat(runnables, contains(NO_OP_RUNNABLE));

    pushBack.run();

    assertThat(runnables, contains(NO_OP_RUNNABLE, pushBack));
    assertThat(runnables.poll(), is(NO_OP_RUNNABLE));
    assertThat(runnables.contains(pushBack), is(true));

    // failed again and pushed back itself to queue
    runnables.poll().run();
    assertThat(runnables.contains(pushBack), is(true));

    runnables.poll().run();

    assertThat(runnables.isEmpty(), is(true));
    assertThat(senders.get(sender), is(0L));
    assertThat(connectedSenders, contains(sender));

    verify(sender, times(3)).onDisconnected();
    verify(sender, times(1)).onConnected();
  }
}
