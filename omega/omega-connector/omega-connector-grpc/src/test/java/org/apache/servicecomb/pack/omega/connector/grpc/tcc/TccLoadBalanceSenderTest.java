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

package org.apache.servicecomb.pack.omega.connector.grpc.tcc;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.apache.servicecomb.pack.contract.grpc.GrpcParticipationStartedEvent;
import org.apache.servicecomb.pack.omega.connector.grpc.LoadBalanceSenderTestBase;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextBuilder;
import org.apache.servicecomb.pack.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationStartedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccStartedEvent;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TccLoadBalanceSenderTest extends LoadBalanceSenderTestBase {
  private final AlphaClusterConfig clusterConfig = mock(AlphaClusterConfig.class);
  private final TccMessageHandler tccMessageHandler = mock(CoordinateMessageHandler.class);

  protected final String[] addresses = {"localhost:8080", "localhost:8090"};

  private LoadBalanceContext loadContext;
  private TccLoadBalanceSender tccLoadBalanceSender;

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String methodName = uniquify("methodName");
  private final String confirmMethod = uniquify("confirmMethod");
  private final String cancelMethod = uniquify("canceMethod");
  private final String serviceName = uniquify("serviceName");

  private final ServiceConfig serviceConfig = new ServiceConfig(serviceName);

  private ParticipationStartedEvent participationStartedEvent;
  private TccStartedEvent tccStartedEvent;
  private TccEndedEvent tccEndedEvent;
  private CoordinatedEvent coordinatedEvent;

  @BeforeClass
  public static void startServer() {
    for (Integer each : ports) {
      startServerOnPort(each);
    }
  }

  @AfterClass
  public static void shutdownServer() {
    for(Server server: servers.values()) {
      server.shutdown();
    }
  }

  private static void startServerOnPort(int port) {
    ServerBuilder<?> serverBuilder = NettyServerBuilder.forAddress(
        new InetSocketAddress("127.0.0.1", port));
    serverBuilder.addService(new MyTccEventServiceImpl(connected.get(port), eventsMap.get(port), delays.get(port)));
    Server server = serverBuilder.build();

    try {
      server.start();
      servers.put(port, server);
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  @Before
  public void setup() {
    when(clusterConfig.getAddresses()).thenReturn(Lists.newArrayList(addresses));
    when(clusterConfig.getTccMessageHandler()).thenReturn(tccMessageHandler);
    when(clusterConfig.isEnableSSL()).thenReturn(false);

    loadContext =
        new LoadBalanceContextBuilder(TransactionType.TCC, clusterConfig, serviceConfig, 30, 4).build();
    tccLoadBalanceSender = new TccLoadBalanceSender(loadContext, new FastestSender());
    participationStartedEvent = new ParticipationStartedEvent(globalTxId, localTxId, parentTxId, confirmMethod,
        cancelMethod);
    tccStartedEvent = new TccStartedEvent(globalTxId, localTxId);
    tccEndedEvent = new TccEndedEvent(globalTxId, localTxId, TransactionStatus.Succeed);
    coordinatedEvent = new CoordinatedEvent(globalTxId, localTxId, parentTxId, methodName, TransactionStatus.Succeed);
  }

  @After
  public void teardown() {
    tccLoadBalanceSender.close();

    for (Queue<Object> queue :eventsMap.values()) {
      queue.clear();
    }
    for (Queue<String> queue :connected.values()) {
      queue.clear();
    }
  }

  @Test
  public void participatedSucceed() {
    Iterator<Long> iterator = loadContext.getSenders().values().iterator();
    assertThat(iterator.next(), is(0L));
    assertThat(iterator.next(), is(0L));

    Iterator<MessageSender> keySet = loadContext.getSenders().keySet().iterator();
    loadContext.getSenders().put(keySet.next(), Long.MAX_VALUE);
    TccMessageSender expectSender = (TccMessageSender) keySet.next();

    // assert expected message sender
    TccMessageSender actualSender = tccLoadBalanceSender.pickMessageSender();
    assertThat(actualSender.target(), is(expectSender.target()));

    AlphaResponse response = tccLoadBalanceSender.participationStart(participationStartedEvent);
    assertThat(loadContext.getSenders().get(actualSender), greaterThan(0L));
    assertThat(response.aborted(), is(false));

    Integer expectPort = Integer.valueOf(expectSender.target().split(":")[1]);
    GrpcParticipationStartedEvent result = (GrpcParticipationStartedEvent) eventsMap.get(expectPort).poll();
    assertThat(result.getGlobalTxId(), is(globalTxId));
    assertThat(result.getCancelMethod(), is(cancelMethod));
    assertThat(result.getConfirmMethod(), is(confirmMethod));
    assertThat(result.getServiceName(), is(serviceName));
    assertThat(result.getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(result.getParentTxId(), is(parentTxId));
    //assertThat(result.getStatus(), is(TransactionStatus.Succeed.name()));
  }

  @Test
  public void participateFailedThenRetry() {
    tccLoadBalanceSender.onConnected();
    await().atMost(2, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return connected.get(8080).size() == 1 && connected.get(8090).size() == 1;
      }
    });
    assertThat((connected.get(8080).size() == 1 && connected.get(8090).size() == 1), is(true));

    // due to 8090 is slow than 8080, so 8080 will be routed with 2 times.
    tccLoadBalanceSender.participationStart(participationStartedEvent);
    tccLoadBalanceSender.participationStart(participationStartedEvent);
    tccLoadBalanceSender.participationStart(participationStartedEvent);
    assertThat(eventsMap.get(8080).size(), is(2));
    assertThat(eventsMap.get(8090).size(), is(1));

    // when 8080 was shutdown, request will be routed to 8090 automatically.
    servers.get(8080).shutdownNow();
    tccLoadBalanceSender.participationStart(participationStartedEvent);
    assertThat(eventsMap.get(8090).size(), is(2));

    // when 8080 was recovery, it will be routed again.
    startServerOnPort(8080);
    await().atMost(3, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return connected.get(8080).size() == 3;
      }
    });
    tccLoadBalanceSender.participationStart(participationStartedEvent);
    assertThat(eventsMap.get(8080).size(), is(3));
  }

  @Test
  public void failFastWhenAllServerWasDown() throws IOException {
    tccLoadBalanceSender.onConnected();
    await().atMost(2, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return connected.get(8080).size() == 1 && connected.get(8090).size() == 1;
      }
    });
    assertThat((connected.get(8080).size() == 1 && connected.get(8090).size() == 1), is(true));

    for (Server each : servers.values()) {
      each.shutdownNow();
    }

    try {
      tccLoadBalanceSender.participationStart(participationStartedEvent);
    } catch (OmegaException ex) {
      assertThat(ex.getMessage().endsWith("all alpha server is down."), is(true));
    }
    for (Integer each : ports) {
      startServerOnPort(each);
    }
  }

  @Test(expected = OmegaException.class)
  public void participateFailedThenAbort() {
    TccMessageSender failedSender = mock(GrpcTccClientMessageSender.class);
    doThrow(new OmegaException("omega exception")).when(failedSender).participationStart((ParticipationStartedEvent)any());
    TccMessageSender succeedSender = mock(GrpcTccClientMessageSender.class);
    when(succeedSender.participationStart((ParticipationStartedEvent) any())).thenReturn(new AlphaResponse(false));

    Map<MessageSender, Long> senders = Maps.newConcurrentMap();
    senders.put(failedSender, 0l);
    senders.put(succeedSender, 10l);
    loadContext.setSenders(senders);
    tccLoadBalanceSender.participationStart(participationStartedEvent);
  }

  @Test
  public void participateInterruptedFailed() throws InterruptedException {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          await().atLeast(1, SECONDS);
          tccLoadBalanceSender.participationStart(participationStartedEvent);
        } catch (OmegaException e) {
          assertThat(e.getMessage().endsWith("interruption"), Is.is(true));
        }
      }
    });
    thread.start();
    thread.interrupt();
    thread.join();
  }

  @Test
  public void TccStartSucceed() {
    TccMessageSender actualSender = tccLoadBalanceSender.pickMessageSender();
    AlphaResponse response = tccLoadBalanceSender.tccTransactionStart(tccStartedEvent);
    assertThat(loadContext.getSenders().get(actualSender), greaterThan(0L));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void TccEndSucceed() {
    TccMessageSender actualSender = tccLoadBalanceSender.pickMessageSender();
    AlphaResponse response = tccLoadBalanceSender.tccTransactionStop(tccEndedEvent);
    assertThat(loadContext.getSenders().get(actualSender), greaterThan(0L));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void TccCoordinatedSucceed() {
    TccMessageSender actualSender = tccLoadBalanceSender.pickMessageSender();
    AlphaResponse response = tccLoadBalanceSender.coordinate(coordinatedEvent);
    assertThat(loadContext.getSenders().get(actualSender), greaterThan(0L));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void broadcastConnectionAndDisconnection() {
    tccLoadBalanceSender.onConnected();
    await().atMost(1, SECONDS).until(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        return !connected.get(8080).isEmpty() && !connected.get(8090).isEmpty();
      }
    });

    Assert.assertThat(connected.get(8080), contains("Connected " + serviceName));
    Assert.assertThat(connected.get(8090), contains("Connected " + serviceName));

    tccLoadBalanceSender.onDisconnected();
    Assert.assertThat(connected.get(8080), contains("Connected " + serviceName, "Disconnected " + serviceName));
    Assert.assertThat(connected.get(8090), contains("Connected " + serviceName, "Disconnected " + serviceName));
  }
}
