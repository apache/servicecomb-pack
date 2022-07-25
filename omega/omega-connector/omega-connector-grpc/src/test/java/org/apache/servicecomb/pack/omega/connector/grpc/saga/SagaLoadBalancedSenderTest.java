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

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.lang.Thread.State.TERMINATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextBuilder;
import org.apache.servicecomb.pack.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.TxAbortedEvent;
import org.apache.servicecomb.pack.omega.transaction.TxEvent;
import org.apache.servicecomb.pack.omega.transaction.TxStartedEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class SagaLoadBalancedSenderTest extends SagaLoadBalancedSenderTestBase {
  @Override
  protected SagaLoadBalanceSender newMessageSender(String[] addresses) {
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(ImmutableList.copyOf(addresses))
        .enableSSL(false)
        .enableMutualAuth(false)
        .messageSerializer(serializer)
        .messageDeserializer(deserializer)
        .messageHandler(handler)
        .build();

    LoadBalanceContext loadContext = new LoadBalanceContextBuilder(
        TransactionType.SAGA,
        clusterConfig,
        new ServiceConfig(serviceName), 100, 4).build();

    return new SagaLoadBalanceSender(loadContext, new FastestSender());
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    for(int port: ports) {
      startServerOnPort(port);
    }
  }

  private static void startServerOnPort(int port) {
    ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
    serverBuilder.addService(new MyTxEventService(connected.get(port), eventsMap.get(port), delays.get(port)));
    Server server = serverBuilder.build();

    try {
      server.start();
      servers.put(port, server);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void resendToAnotherServerOnFailure() throws Exception {
    messageSender.send(event);

    int deadPort = killServerReceivedMessage();

    messageSender.send(event);
    messageSender.send(event);

    assertThat(eventsMap.get(deadPort).size(), is(1));
    assertThat(eventsMap.get(deadPort).peek().toString(), is(event.toString()));

    int livePort = deadPort == 8080 ? 8090 : 8080;
    assertThat(eventsMap.get(livePort).size(), is(2));
    assertThat(eventsMap.get(livePort).peek().toString(), is(event.toString()));

    // restart killed server in order not to affect other tests
    startServerOnPort(deadPort);
  }

  @Test
  public void resetLatencyOnReconnection() throws Exception {
    messageSender.onConnected();
    messageSender.send(event);

    final int deadPort = killServerReceivedMessage();

    // ensure live message sender has latency greater than 0
    messageSender.send(event);

    startServerOnPort(deadPort);
    await().atMost(120, SECONDS).until(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        return connected.get(deadPort).size() == 3;
      }
    });

    TxEvent abortedEvent = new TxAbortedEvent(globalTxId, localTxId, parentTxId, compensationMethod, new RuntimeException("oops"));
    messageSender.send(abortedEvent);

    // restarted server gets priority, since it had no traffic
    assertThat(eventsMap.get(deadPort).size(), is(2));
    assertThat(eventsMap.get(deadPort).poll().toString(), is(event.toString()));
    assertThat(eventsMap.get(deadPort).poll().toString(), is(abortedEvent.toString()));

    await().atMost(3, SECONDS).until(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        return compensated.contains(globalTxId);
      }
    });
  }

  @Test
  public void stopSendingOnInterruption() throws Exception {
    SagaMessageSender underlying = Mockito.mock(SagaMessageSender.class);
    doThrow(RuntimeException.class).when(underlying).send(event);

    setSenders(underlying);
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          messageSender.send(event);
        } catch (Exception ex) {
          assertThat(ex.getMessage().endsWith("Failed to get reconnected sender"), is(true));
        }
      }
    });
    thread.start();

    Thread.sleep(300);

    // stop trying to send message out on exception
    verify(underlying, times(1)).send(event);

    thread.interrupt();
    thread.join();
  }

  @Test
  public void broadcastConnectionAndDisconnection() throws Exception {
    messageSender.onConnected();
    await().atMost(1, SECONDS).until(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        return !connected.get(8080).isEmpty() && !connected.get(8090).isEmpty();
      }
    });

    assertThat(connected.get(8080), contains("Connected " + serviceName));
    assertThat(connected.get(8090), contains("Connected " + serviceName));

    messageSender.onDisconnected();
    assertThat(connected.get(8080), contains("Connected " + serviceName, "Disconnected " + serviceName));
    assertThat(connected.get(8090), contains("Connected " + serviceName, "Disconnected " + serviceName));
  }

  @Test
  public void swallowException_UntilAllSendersConnected() throws Exception {
    SagaMessageSender underlying1 = Mockito.mock(SagaMessageSender.class);
    doThrow(RuntimeException.class).when(underlying1).onConnected();

    SagaMessageSender underlying2 = Mockito.mock(SagaMessageSender.class);

    setSenders(underlying1, underlying2);

    messageSender.onConnected();

    verify(underlying1).onConnected();
    verify(underlying2).onConnected();
  }

  @Test
  public void swallowException_UntilAllSendersDisconnected() throws Exception {
    SagaMessageSender underlying1 = Mockito.mock(SagaMessageSender.class);
    doThrow(RuntimeException.class).when(underlying1).onDisconnected();

    SagaMessageSender underlying2 = Mockito.mock(SagaMessageSender.class);

    setSenders(underlying1, underlying2);

    messageSender.onDisconnected();

    verify(underlying1).onDisconnected();
    verify(underlying2).onDisconnected();
  }

  @Test
  public void considerFasterServerFirst() throws Exception {
    // we don't know which server is selected at first
    messageSender.send(event);

    // but now we only send to the one with lowest latency
    messageSender.send(event);
    messageSender.send(event);
    messageSender.send(event);

    assertThat(eventsMap.get(8080).size(), is(3));
    assertThat(eventsMap.get(8090).size(), is(1));
  }

  @Test
  public void blowsUpWhenNoServerAddressProvided() throws Exception {
    try {
      newMessageSender(new String[0]);
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("No reachable cluster address provided"));
    }
  }

  @Test
  public void stopSendingWhenClusterIsDown() throws Exception {
    for(Server server:servers.values()) {
      server.shutdownNow();
    }
    messageSender.onConnected();

    final Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          messageSender.send(event);
        } catch (OmegaException ex) {
          assertThat(ex.getMessage().endsWith("all alpha server is down."), is(true));
        }
      }
    });
    thread.start();

    // we don't want to keep sending on cluster down
    await().atMost(30, SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return thread.getState().equals(TERMINATED);
      }
    });

    assertThat(eventsMap.get(8080).isEmpty(), is(true));
    assertThat(eventsMap.get(8090).isEmpty(), is(true));

    //TODO:it seems in Windows environment we need wait a short time in order to make sure reconnect mechanism work
    Thread.sleep(2000);

    startServerOnPort(8080);
    startServerOnPort(8090);

    await().atMost(10,SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return messageSender.pickMessageSender()!=null;
      }
    });
    messageSender.send(event);
    await().atMost(2, SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return connected.get(8080).size() == 2 || connected.get(8090).size() == 2;
      }
    });

    await().atMost(2, SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return eventsMap.get(8080).size() == 1 || eventsMap.get(8090).size() == 1;
      }
    });
  }

  @Test
  public void forwardSendResult() {
    assertThat(messageSender.send(event).aborted(), is(false));

    TxEvent rejectEvent = new TxStartedEvent(globalTxId, localTxId, parentTxId, "reject", 0, "", 0,
        0, 0, 0, 0, "blah");
    assertThat(messageSender.send(rejectEvent).aborted(), is(true));
  }

  @Test
  public void blowsUpWhenServerIsInterrupted() throws InterruptedException {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          messageSender.send(event);
          expectFailing(OmegaException.class);
        } catch (OmegaException e) {
          assertThat(e.getMessage().endsWith("interruption"), is(true));
        }
      }
    });

    thread.start();
    thread.interrupt();
    thread.join();
  }

  private int killServerReceivedMessage() {
    for (int port : eventsMap.keySet()) {
      if (!eventsMap.get(port).isEmpty()) {
        Server serverToKill = servers.get(port);
        serverToKill.shutdownNow();
        return port;
      }
    }
    throw new IllegalStateException("None of the servers received any message");
  }

  private void setSenders(SagaMessageSender ... underlyings) {
    Map<MessageSender, Long> senders = new HashMap<>();
    for (SagaMessageSender each : underlyings) {
      senders.put(each, 0L);
    }
    messageSender.getLoadContext().setSenders(senders);
  }
}
