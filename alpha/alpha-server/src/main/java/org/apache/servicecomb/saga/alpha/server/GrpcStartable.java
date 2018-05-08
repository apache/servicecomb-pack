/*
 *
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
 *
 *
 */

package org.apache.servicecomb.saga.alpha.server;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

class GrpcStartable implements ServerStartable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Server server;

  GrpcStartable(GrpcServerConfig serverConfig, BindableService... services) {
    ServerBuilder<?> serverBuilder;
    if (serverConfig.isEnable()){
      serverBuilder = NettyServerBuilder.forAddress(
          new InetSocketAddress(serverConfig.getHost(), serverConfig.getPort()));

      try {
        ((NettyServerBuilder) serverBuilder).sslContext(getSslContextBuilder(serverConfig).build());
      } catch (SSLException e) {
        throw new IllegalStateException("Unable to setup grpc to use SSL.", e);
      }
    } else {
      serverBuilder = ServerBuilder.forPort(serverConfig.getPort());
    }
    Arrays.stream(services).forEach(serverBuilder::addService);
    server = serverBuilder.build();
  }

  @Override
  public void start() {
    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

    try {
      server.start();
      server.awaitTermination();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to start grpc server.", e);
    } catch (InterruptedException e) {
      LOG.error("grpc server was interrupted.", e);
      Thread.currentThread().interrupt();
    }
  }

  private SslContextBuilder getSslContextBuilder(GrpcServerConfig config) {

    Properties prop = new Properties();
    try {
      prop.load(getClass().getClassLoader().getResourceAsStream("ssl.properties"));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read ssl.properties.", e);
    }

    SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(config.getCert()),
        new File(config.getKey()))
        .protocols(prop.getProperty("protocols"))
        .ciphers(Arrays.asList(prop.getProperty("ciphers").split(",")));
    if (config.isMutalAuth()) {
      sslClientContextBuilder.trustManager(new File(config.getClientCert()));
      sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
    }
    return GrpcSslContexts.configure(sslClientContextBuilder,
        SslProvider.OPENSSL);
  }
}
