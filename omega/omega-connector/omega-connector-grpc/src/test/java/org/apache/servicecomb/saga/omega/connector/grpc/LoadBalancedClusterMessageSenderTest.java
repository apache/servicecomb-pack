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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceImplBase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoadBalancedClusterMessageSenderTest {

  private static final int[] ports = {8080, 8090};
  private static final List<Server> servers = new ArrayList<>();

  private static final Queue<TxEvent> eventsOn8080 = new ConcurrentLinkedQueue<>();
  private static final Queue<TxEvent> eventsOn8090 = new ConcurrentLinkedQueue<>();

  private static final Map<Integer, Integer> delays = new HashMap<Integer, Integer>() {{
    put(8080, 0);
    put(8090, 100);
  }};

  private static final Map<Integer, Set<String>> connected = new HashMap<Integer, Set<String>>() {{
    put(8080, new ConcurrentSkipListSet<>());
    put(8090, new ConcurrentSkipListSet<>());
  }};

  private static final Map<Integer, Queue<TxEvent>> eventsMap = new HashMap<Integer, Queue<TxEvent>>() {{
    put(8080, eventsOn8080);
    put(8090, eventsOn8090);
  }};

  private final MessageSerializer serializer = objects -> objects[0].toString().getBytes();

  private final MessageDeserializer deserializer = message -> new Object[] {new String(message)};
  private final MessageHandler handler = (globalTxId, localTxId, parentTxId, compensationMethod, payloads) -> {
  };

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String compensationMethod = getClass().getCanonicalName();
  private final TxEvent event = new TxEvent(globalTxId, localTxId, parentTxId, compensationMethod, "blah");

  private final String serviceName = uniquify("serviceName");
  private final String[] addresses = {"localhost:8080", "localhost:8090"};
  private final MessageSender messageSender = newMessageSender(addresses);

  private MessageSender newMessageSender(String[] addresses) {
    return new LoadBalancedClusterMessageSender(
        addresses,
        serializer,
        deserializer,
        new ServiceConfig(serviceName),
        handler);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    Arrays.stream(ports).forEach(port -> {
      ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
      serverBuilder.addService(new MyTxEventService(connected.get(port), eventsMap.get(port), delays.get(port)));
      Server server = serverBuilder.build();

      try {
        server.start();
        servers.add(server);
      } catch (IOException e) {
        fail(e.getMessage());
      }
    });
  }

  @AfterClass
  public static void tearDown() throws Exception {
    servers.forEach(Server::shutdown);
  }

  @After
  public void after() throws Exception {
    eventsOn8080.clear();
    eventsOn8090.clear();
  }

  @Test
  public void resendToAnotherServerOnFailure() throws Exception {
    messageSender.send(event);

    killServerReceivedMessage();

    messageSender.send(event);

    assertThat(eventsOn8080.size(), is(1));
    assertThat(eventsOn8080.peek().toString(), is(event.toString()));

    assertThat(eventsOn8090.size(), is(1));
    assertThat(eventsOn8090.peek().toString(), is(event.toString()));
  }

  @Test (timeout = 1000)
  public void stopSendingOnInterruption() throws Exception {
    MessageSender underlying = Mockito.mock(MessageSender.class);
    doThrow(RuntimeException.class).when(underlying).send(event);

    MessageSender messageSender = new LoadBalancedClusterMessageSender(underlying);

    Thread thread = new Thread(() -> messageSender.send(event));
    thread.start();

    Thread.sleep(300);

    thread.interrupt();
    thread.join();
  }

  @Test
  public void broadcastConnectionAndDisconnection() throws Exception {
    messageSender.onConnected();
    await().atMost(1, SECONDS).until(() -> !connected.get(8080).isEmpty() && !connected.get(8090).isEmpty());

    assertThat(connected.get(8080), contains(serviceName));
    assertThat(connected.get(8090), contains(serviceName));

    messageSender.onDisconnected();
    assertThat(connected.get(8080).isEmpty(), is(true));
    assertThat(connected.get(8090).isEmpty(), is(true));
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

    assertThat(eventsOn8080.size(), greaterThanOrEqualTo(2));
    assertThat(eventsOn8090.size(), lessThanOrEqualTo(1));
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

  private void killServerReceivedMessage() {
    int index = 0;
    for (int port : eventsMap.keySet()) {
      if (!eventsMap.get(port).isEmpty()) {
        servers.get(index).shutdownNow();
      }
      index++;
    }
  }

  private static class MyTxEventService extends TxEventServiceImplBase {
    private final Set<String> connected;
    private final Queue<TxEvent> events;
    private final int delay;

    private MyTxEventService(Set<String> connected, Queue<TxEvent> events, int delay) {
      this.connected = connected;
      this.events = events;
      this.delay = delay;
    }

    @Override
    public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcCompensateCommand> responseObserver) {
      connected.add(request.getServiceName());
    }

    @Override
    public void onTxEvent(GrpcTxEvent request, StreamObserver<GrpcAck> responseObserver) {
      events.offer(new TxEvent(
          request.getGlobalTxId(),
          request.getLocalTxId(),
          request.getParentTxId(),
          request.getCompensationMethod(),
          new String(request.getPayloads().toByteArray())));

      sleep();

      responseObserver.onNext(GrpcAck.newBuilder().build());
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
      connected.remove(request.getServiceName());
      responseObserver.onNext(GrpcAck.newBuilder().build());
      responseObserver.onCompleted();
    }
  }
}
