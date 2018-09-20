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
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.saga.omega.connector.grpc.FastestSender;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.hamcrest.core.Is;
import org.junit.After;
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
  private final String cancelMethod = uniquify("cancleMethod");
  private final String serviceName = uniquify("serviceName");

  private final ServiceConfig serviceConfig = new ServiceConfig(serviceName);

  private ParticipatedEvent participatedEvent;
  private TccStartedEvent tccStartedEvent;
  private TccEndedEvent tccEndedEvent;
  private CoordinatedEvent coordinatedEvent;

  @BeforeClass
  public static void startServer() throws IOException {
    for (Integer each : ports) {
      startServerOnPort(each);
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
        new LoadBalanceContextBuilder(TransactionType.TCC, clusterConfig, serviceConfig, 30).build();
    tccLoadBalanceSender = new TccLoadBalanceSender(loadContext, new FastestSender());
    participatedEvent = new ParticipatedEvent(globalTxId, localTxId, parentTxId, confirmMethod, cancelMethod, TransactionStatus.Succeed);
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

    AlphaResponse response = tccLoadBalanceSender.participate(participatedEvent);
    assertThat(loadContext.getSenders().get(actualSender), greaterThan(0L));
    assertThat(response.aborted(), is(false));

    Integer expectPort = Integer.valueOf(expectSender.target().split(":")[1]);
    GrpcTccParticipatedEvent result = (GrpcTccParticipatedEvent) eventsMap.get(expectPort).poll();
    assertThat(result.getGlobalTxId(), is(globalTxId));
    assertThat(result.getCancelMethod(), is(cancelMethod));
    assertThat(result.getConfirmMethod(), is(confirmMethod));
    assertThat(result.getServiceName(), is(serviceName));
    assertThat(result.getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(result.getParentTxId(), is(parentTxId));
    assertThat(result.getStatus(), is(TransactionStatus.Succeed.name()));
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
    tccLoadBalanceSender.participate(participatedEvent);
    tccLoadBalanceSender.participate(participatedEvent);
    tccLoadBalanceSender.participate(participatedEvent);
    assertThat(eventsMap.get(8080).size(), is(2));
    assertThat(eventsMap.get(8090).size(), is(1));

    // when 8080 was shutdown, request will be routed to 8090 automatically.
    servers.get(8080).shutdownNow();
    tccLoadBalanceSender.participate(participatedEvent);
    assertThat(eventsMap.get(8090).size(), is(2));

    // when 8080 was recovery, it will be routed again.
    startServerOnPort(8080);
    await().atMost(2, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return connected.get(8080).size() == 3;
      }
    });
    tccLoadBalanceSender.participate(participatedEvent);
    assertThat(eventsMap.get(8080).size(), is(3));
  }

  @Test(expected = OmegaException.class)
  public void participateFailedThenAbort() {
    TccMessageSender failedSender = mock(GrpcTccClientMessageSender.class);
    doThrow(new OmegaException("omega exception")).when(failedSender).participate((ParticipatedEvent)any());
    TccMessageSender succeedSender = mock(GrpcTccClientMessageSender.class);
    when(succeedSender.participate((ParticipatedEvent) any())).thenReturn(new AlphaResponse(false));

    Map<MessageSender, Long> senders = Maps.newConcurrentMap();
    senders.put(failedSender, 0l);
    senders.put(succeedSender, 10l);
    loadContext.setSenders(senders);
    tccLoadBalanceSender.participate(participatedEvent);
  }

  @Test
  public void participateInterruptedFailed() throws InterruptedException {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          await().atLeast(1, SECONDS);
          tccLoadBalanceSender.participate(participatedEvent);
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
