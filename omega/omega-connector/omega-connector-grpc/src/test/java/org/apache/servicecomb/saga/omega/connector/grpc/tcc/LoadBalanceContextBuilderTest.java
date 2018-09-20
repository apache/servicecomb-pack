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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.servicecomb.saga.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LoadBalanceContextBuilderTest {

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final AlphaClusterConfig clusterConfig = mock(AlphaClusterConfig.class);
  private final TccMessageHandler tccMessageHandler = mock(CoordinateMessageHandler.class);
  private final String serverName = uniquify("serviceName");
  private final ServiceConfig serviceConfig = new ServiceConfig(serverName);
  protected final String[] addresses = {"localhost:8080", "localhost:8090"};

  private  LoadBalanceContextBuilder tccLoadBalanceContextBuilder;
  private  LoadBalanceContextBuilder sagaLoadBalanceContextBuilder;

  @Before
  public void setup() throws IOException {
    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName("localhost:8080").directExecutor().build().start());
    grpcCleanup.register(InProcessServerBuilder.forName("localhost:8090").directExecutor().build().start());
    when(clusterConfig.getAddresses()).thenReturn(Lists.newArrayList(addresses));
    when(clusterConfig.getTccMessageHandler()).thenReturn(tccMessageHandler);
    tccLoadBalanceContextBuilder =
        new LoadBalanceContextBuilder(TransactionType.TCC, clusterConfig, serviceConfig, 30);
    sagaLoadBalanceContextBuilder =
        new LoadBalanceContextBuilder(TransactionType.SAGA, clusterConfig, serviceConfig, 30);
  }

  @After
  public void teardown() {
  }

  @Test
  public void buildTccLoadBalanceContextWithoutSsl() {
    when(clusterConfig.isEnableSSL()).thenReturn(false);

    LoadBalanceContext loadContext = tccLoadBalanceContextBuilder.build();
    assertThat(loadContext.getPendingTaskRunner().getReconnectDelay(), is(30));
    assertThat(loadContext.getSenders().size(), is(2));
    assertThat(loadContext.getSenders().keySet().iterator().next(), instanceOf(TccMessageSender.class));
    assertThat(loadContext.getSenders().values().iterator().next(), is(0l));
    assertThat(loadContext.getChannels().size(), is(2));
    loadContext.getSenders().keySet().iterator().next().close();
    shutdownChannels(loadContext);
  }

  @Test
  public void buildTccLoadBalanceContextWithSsl() {
    when(clusterConfig.isEnableSSL()).thenReturn(true);
    when(clusterConfig.getCert()).thenReturn(getClass().getClassLoader().getResource("client.crt").getFile());
    when(clusterConfig.getCertChain()).thenReturn(getClass().getClassLoader().getResource("ca.crt").getFile());
    when(clusterConfig.getKey()).thenReturn(getClass().getClassLoader().getResource("client.pem").getFile());
    LoadBalanceContext loadContext = tccLoadBalanceContextBuilder.build();
    assertThat(loadContext.getPendingTaskRunner().getReconnectDelay(), is(30));
    assertThat(loadContext.getSenders().size(), is(2));
    assertThat(loadContext.getSenders().keySet().iterator().next(), instanceOf(TccMessageSender.class));
    assertThat(loadContext.getSenders().values().iterator().next(), is(0l));
    assertThat(loadContext.getChannels().size(), is(2));
    shutdownChannels(loadContext);
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwExceptionWhenAddressIsNotExist() {
    when(clusterConfig.getAddresses()).thenReturn(new ArrayList<String>());
    tccLoadBalanceContextBuilder.build();
  }

  @Test
  public void buildSagaLoadBalanceContextWithoutSsl() {

  }

  @Test
  public void buildSagaLoadBalanceContextWithSsl() {

  }

  private void shutdownChannels(LoadBalanceContext loadContext) {
    for (ManagedChannel each : loadContext.getChannels()) {
      each.shutdownNow();
    }
  }
}
