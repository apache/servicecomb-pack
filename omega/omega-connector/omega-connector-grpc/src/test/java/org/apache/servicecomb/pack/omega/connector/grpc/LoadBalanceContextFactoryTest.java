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

package org.apache.servicecomb.pack.omega.connector.grpc;

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
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextFactory;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LoadBalanceContextFactoryTest {

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final AlphaClusterConfig clusterConfig = mock(AlphaClusterConfig.class);

  private final TccMessageHandler tccMessageHandler = mock(CoordinateMessageHandler.class);

  private final String serverName = uniquify("serviceName");

  private final ServiceConfig serviceConfig = new ServiceConfig(serverName);

  private final String[] addresses = {"localhost:8080", "localhost:8090"};

  private LoadBalanceContext loadContext;

  @Before
  public void setup() throws IOException {
    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName("localhost:8080").directExecutor().build().start());
    grpcCleanup.register(InProcessServerBuilder.forName("localhost:8090").directExecutor().build().start());
    when(clusterConfig.getAddresses()).thenReturn(Lists.newArrayList(addresses));
    when(clusterConfig.getTccMessageHandler()).thenReturn(tccMessageHandler);
    loadContext = LoadBalanceContextFactory.newInstance(TransactionType.TCC, clusterConfig,
        serviceConfig, 30, 10);
  }

  @After
  public void teardown() {
  }

  @Test
  public void buildTccLoadBalanceContextWithoutSsl() {
    when(clusterConfig.isEnableSSL()).thenReturn(false);
    assertThat(loadContext.getSenders().size(), is(2));
    assertThat(loadContext.getSenders().keySet().iterator().next(), instanceOf(TccMessageSender.class));
    assertThat(loadContext.getSenders().values().iterator().next(), is(0L));
    assertThat(loadContext.getChannels().size(), is(2));
    loadContext.getSenders().keySet().iterator().next().close();
    shutdownChannels(loadContext);
  }

  private void shutdownChannels(LoadBalanceContext loadContext) {
    for (ManagedChannel each : loadContext.getChannels()) {
      each.shutdownNow();
    }
  }
}
