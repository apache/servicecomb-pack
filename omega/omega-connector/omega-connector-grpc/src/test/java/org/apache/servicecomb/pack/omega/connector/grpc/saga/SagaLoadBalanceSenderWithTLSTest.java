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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Callable;
import javax.net.ssl.SSLException;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextBuilder;
import org.apache.servicecomb.pack.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.junit.BeforeClass;
import org.junit.Test;

public class SagaLoadBalanceSenderWithTLSTest extends SagaLoadBalancedSenderTestBase {

  @Override
  protected SagaLoadBalanceSender newMessageSender(String[] addresses) {
    ClassLoader classLoader = getClass().getClassLoader();
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(ImmutableList.copyOf(addresses))
        .enableMutualAuth(true)
        .enableSSL(true)
        .cert(classLoader.getResource("client.crt").getFile())
        .messageHandler(handler)
        .key(classLoader.getResource("client.pem").getFile())
        .certChain(classLoader.getResource("ca.crt").getFile())
        .messageSerializer(serializer)
        .messageDeserializer(deserializer)
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
      System.out.println("Start the port " + port);
      startServerOnPort(port);
    }
  }

  private static void startServerOnPort(int port) {
    ServerBuilder<?> serverBuilder = NettyServerBuilder.forAddress(
        new InetSocketAddress("127.0.0.1", port));
    try {
      ((NettyServerBuilder) serverBuilder).sslContext(getSslContextBuilder().build());
    } catch (SSLException e) {
      throw new IllegalStateException("Unable to setup grpc to use SSL.", e);
    }

    serverBuilder.addService(new MyTxEventService(connected.get(port), eventsMap.get(port), delays.get(port)));
    Server server = serverBuilder.build();

    try {
      server.start();
      servers.put(port, server);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private static SslContextBuilder getSslContextBuilder() {
    ClassLoader classLoader = SagaLoadBalanceSenderWithTLSTest.class.getClassLoader();
    SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(
        new File(classLoader.getResource("server.crt").getFile()),
        new File(classLoader.getResource("server.pem").getFile()))
        .protocols("TLSv1.2","TLSv1.1")
        .ciphers(Arrays.asList("ECDHE-RSA-AES128-GCM-SHA256",
            "ECDHE-RSA-AES256-GCM-SHA384"));

      sslClientContextBuilder.trustManager(new File(classLoader.getResource("client.crt").getFile()));
      sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);

    return GrpcSslContexts.configure(sslClientContextBuilder,
        SslProvider.OPENSSL);
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
  public void broadcastConnectionAndDisconnection() {
    messageSender.onConnected();
    await().atMost(1, SECONDS).until( new Callable<Boolean>() {
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
}
