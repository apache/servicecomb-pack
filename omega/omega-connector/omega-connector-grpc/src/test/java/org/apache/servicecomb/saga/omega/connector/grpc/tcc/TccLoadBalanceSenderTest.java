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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
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
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TccLoadBalanceSenderTest {
  private final AlphaClusterConfig clusterConfig = mock(AlphaClusterConfig.class);
  private final TccMessageHandler tccMessageHandler = mock(CoordinateMessageHandler.class);
  private final String serverName = uniquify("serviceName");
  private final ServiceConfig serviceConfig = new ServiceConfig(serverName);
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

  private ParticipatedEvent participatedEvent;
  private TccStartedEvent tccStartedEvent;
  private TccEndedEvent tccEndedEvent;
  private CoordinatedEvent coordinatedEvent;

  @BeforeClass
  public static void startServer() throws IOException {
    startServerOnPort(8080);
    startServerOnPort(8090);
  }

  private static void startServerOnPort(int port) throws IOException {
    ServerBuilder<?> serverBuilder = NettyServerBuilder.forAddress(
        new InetSocketAddress("127.0.0.1", port));
    serverBuilder.addService(new MyTccEventServiceImpl());
    Server server = serverBuilder.build();
    server.start();
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
  }

  @Test
  public void participateFailedThenRetry() {
    TccMessageSender failedSender = mock(GrpcTccClientMessageSender.class);
    doThrow(new IllegalArgumentException()).when(failedSender).participate((ParticipatedEvent)any());
    TccMessageSender succeedSender = mock(GrpcTccClientMessageSender.class);
    when(succeedSender.participate((ParticipatedEvent) any())).thenReturn(new AlphaResponse(false));

    Map<MessageSender, Long> senders = Maps.newConcurrentMap();
    senders.put(failedSender, 0l);
    senders.put(succeedSender, 10l);
    loadContext.setSenders(senders);

    AlphaResponse response = tccLoadBalanceSender.participate(participatedEvent);
    assertThat(response.aborted(), is(false));
    assertThat(loadContext.getSenders().get(failedSender), is(Long.MAX_VALUE));
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
  public void onConnected() {
    tccLoadBalanceSender.onConnected();
  }

  @Test
  public void onDisconnect() {
    tccLoadBalanceSender.onConnected();
    tccLoadBalanceSender.onDisconnected();
  }
}
