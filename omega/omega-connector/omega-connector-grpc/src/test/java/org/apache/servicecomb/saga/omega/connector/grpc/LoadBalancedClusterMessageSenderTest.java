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
import static java.lang.Thread.State.WAITING;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.TxAbortedEvent;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.omega.transaction.TxStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class LoadBalancedClusterMessageSenderTest {

  private static final int[] ports = {8080, 8090};
  private static final Map<Integer, Server> servers = new HashMap<>();

  private static final Map<Integer, Integer> delays = new HashMap<Integer, Integer>() {{
    put(8080, 0);
    put(8090, 800);
  }};

  private static final Map<Integer, Queue<String>> connected = new HashMap<Integer, Queue<String>>() {{
    put(8080, new ConcurrentLinkedQueue<>());
    put(8090, new ConcurrentLinkedQueue<>());
  }};

  private static final Map<Integer, Queue<TxEvent>> eventsMap = new HashMap<Integer, Queue<TxEvent>>() {{
    put(8080, new ConcurrentLinkedQueue<>());
    put(8090, new ConcurrentLinkedQueue<>());
  }};

  private final MessageSerializer serializer = objects -> objects[0].toString().getBytes();

  private final MessageDeserializer deserializer = message -> new Object[] {new String(message)};

  private final List<String> compensated = new ArrayList<>();

  private final MessageHandler handler = (globalTxId, localTxId, parentTxId, compensationMethod,
      payloads) -> compensated.add(globalTxId);

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String compensationMethod = getClass().getCanonicalName();
  private final TxEvent event = new TxEvent(EventType.TxStartedEvent, globalTxId, localTxId, parentTxId,
      compensationMethod, 0, "", 0, "blah");

  private final String serviceName = uniquify("serviceName");
  private final String[] addresses = {"localhost:8080", "localhost:8090"};
  private final MessageSender messageSender = newMessageSender(addresses);

  private MessageSender newMessageSender(String[] addresses) {
    return new LoadBalancedClusterMessageSender(
        addresses,
        serializer,
        deserializer,
        new ServiceConfig(serviceName),
        handler,
        100);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    Arrays.stream(ports).forEach(LoadBalancedClusterMessageSenderTest::startServerOnPort);
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

  @AfterClass
  public static void tearDown() throws Exception {
    servers.values().forEach(Server::shutdown);
  }

  @After
  public void after() throws Exception {
    messageSender.onDisconnected();
    messageSender.close();
    eventsMap.values().forEach(Queue::clear);
    connected.values().forEach(Queue::clear);
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

    int deadPort = killServerReceivedMessage();

    // ensure live message sender has latency greater than 0
    messageSender.send(event);

    startServerOnPort(deadPort);
    await().atMost(5, SECONDS).until(() -> connected.get(deadPort).size() == 3);

    TxEvent abortedEvent = new TxAbortedEvent(globalTxId, localTxId, parentTxId, compensationMethod, new RuntimeException("oops"));
    messageSender.send(abortedEvent);

    // restarted server gets priority, since it had no traffic
    assertThat(eventsMap.get(deadPort).size(), is(2));
    assertThat(eventsMap.get(deadPort).poll().toString(), is(event.toString()));
    assertThat(eventsMap.get(deadPort).poll().toString(), is(abortedEvent.toString()));

    await().atMost(3, SECONDS).until(() -> compensated.contains(globalTxId));
  }

  @Test(timeout = 1000)
  public void stopSendingOnInterruption() throws Exception {
    MessageSender underlying = Mockito.mock(MessageSender.class);
    doThrow(RuntimeException.class).when(underlying).send(event);

    MessageSender messageSender = new LoadBalancedClusterMessageSender(underlying);

    Thread thread = new Thread(() -> messageSender.send(event));
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
    await().atMost(1, SECONDS).until(() -> !connected.get(8080).isEmpty() && !connected.get(8090).isEmpty());

    assertThat(connected.get(8080), contains("Connected " + serviceName));
    assertThat(connected.get(8090), contains("Connected " + serviceName));

    messageSender.onDisconnected();
    assertThat(connected.get(8080), contains("Connected " + serviceName, "Disconnected " + serviceName));
    assertThat(connected.get(8090), contains("Connected " + serviceName, "Disconnected " + serviceName));
  }

  @Test
  public void swallowException_UntilAllSendersConnected() throws Exception {
    MessageSender underlying1 = Mockito.mock(MessageSender.class);
    doThrow(RuntimeException.class).when(underlying1).onConnected();

    MessageSender underlying2 = Mockito.mock(MessageSender.class);

    MessageSender sender = new LoadBalancedClusterMessageSender(underlying1, underlying2);

    sender.onConnected();

    verify(underlying1).onConnected();
    verify(underlying2).onConnected();
  }

  @Test
  public void swallowException_UntilAllSendersDisconnected() throws Exception {
    MessageSender underlying1 = Mockito.mock(MessageSender.class);
    doThrow(RuntimeException.class).when(underlying1).onDisconnected();

    MessageSender underlying2 = Mockito.mock(MessageSender.class);

    MessageSender sender = new LoadBalancedClusterMessageSender(underlying1, underlying2);

    sender.onDisconnected();

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
    servers.values().forEach(Server::shutdownNow);
    messageSender.onConnected();

    Thread thread = new Thread(() -> messageSender.send(event));
    thread.start();

    // we don't want to keep sending on cluster down
    await().atMost(2, SECONDS).until(() -> thread.isAlive() && thread.getState().equals(WAITING));

    assertThat(eventsMap.get(8080).isEmpty(), is(true));
    assertThat(eventsMap.get(8090).isEmpty(), is(true));

    startServerOnPort(8080);
    startServerOnPort(8090);

    await().atMost(2, SECONDS).until(() -> connected.get(8080).size() == 2 || connected.get(8090).size() == 2);
    await().atMost(2, SECONDS).until(() -> eventsMap.get(8080).size() == 1 || eventsMap.get(8090).size() == 1);
  }

  @Test
  public void forwardSendResult() {
    assertThat(messageSender.send(event).aborted(), is(false));

    TxEvent rejectEvent = new TxStartedEvent(globalTxId, localTxId, parentTxId, "reject", 0, "", 0, "blah");
    assertThat(messageSender.send(rejectEvent).aborted(), is(true));
  }

  @Test
  public void blowsUpWhenServerIsInterrupted() throws InterruptedException {
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

  private static class MyTxEventService extends TxEventServiceImplBase {
    private final Queue<String> connected;
    private final Queue<TxEvent> events;
    private final int delay;

    private StreamObserver<GrpcCompensateCommand> responseObserver;

    private MyTxEventService(Queue<String> connected, Queue<TxEvent> events, int delay) {
      this.connected = connected;
      this.events = events;
      this.delay = delay;
    }

    @Override
    public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcCompensateCommand> responseObserver) {
      this.responseObserver = responseObserver;
      connected.add("Connected " + request.getServiceName());
    }

    @Override
    public void onTxEvent(GrpcTxEvent request, StreamObserver<GrpcAck> responseObserver) {
      events.offer(new TxEvent(
          EventType.valueOf(request.getType()),
          request.getGlobalTxId(),
          request.getLocalTxId(),
          request.getParentTxId(),
          request.getCompensationMethod(),
          request.getTimeout(),
          request.getRetryMethod(),
          request.getRetries(),
          new String(request.getPayloads().toByteArray())));

      sleep();

      if (EventType.TxAbortedEvent.name().equals(request.getType())) {
        this.responseObserver.onNext(GrpcCompensateCommand
            .newBuilder()
            .setGlobalTxId(request.getGlobalTxId())
            .build());
      }

      if ("TxStartedEvent".equals(request.getType()) && request.getCompensationMethod().equals("reject")) {
        responseObserver.onNext(GrpcAck.newBuilder().setAborted(true).build());
      } else {
        responseObserver.onNext(GrpcAck.newBuilder().setAborted(false).build());
      }

      responseObserver.onCompleted();
    }

    private void sleep() {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        fail(e.getMessage());
      }
    }

    @Override
    public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
      connected.add("Disconnected " + request.getServiceName());
      responseObserver.onNext(GrpcAck.newBuilder().build());
      responseObserver.onCompleted();
    }
  }
}
