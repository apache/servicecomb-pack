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

package org.apache.servicecomb.pack.alpha.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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

public class GrpcStartable implements ServerStartable {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Server server;
  private final GrpcServerConfig serverConfig;

  public GrpcStartable(GrpcServerConfig serverConfig, BindableService... services) throws IOException {
    this.serverConfig = serverConfig;
    ServerBuilder<?> serverBuilder;
    try {
      OptionalInt unusedPort = findUnusedPort(serverConfig);
      if(unusedPort.isPresent()){
        if (serverConfig.isSslEnable()){
          serverBuilder = NettyServerBuilder.forAddress(
                  new InetSocketAddress(serverConfig.getHost(), unusedPort.getAsInt()));

          try {
            ((NettyServerBuilder) serverBuilder).sslContext(getSslContextBuilder(serverConfig).build());
          } catch (SSLException e) {
            throw new IllegalStateException("Unable to setup grpc to use SSL.", e);
          }
        } else {
          serverBuilder = ServerBuilder.forPort(unusedPort.getAsInt());
        }
        Arrays.stream(services).forEach(serverBuilder::addService);
        server = serverBuilder.build();
        serverConfig.setPort(unusedPort.getAsInt());
      }
    } catch (IOException e) {
      throw e;
    }
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

  @Override
  public GrpcServerConfig getGrpcServerConfig() {
    return this.serverConfig;
  }

  private SslContextBuilder getSslContextBuilder(GrpcServerConfig config) {

    Properties prop = new Properties();
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      prop.load(classLoader.getResourceAsStream("ssl.properties"));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read ssl.properties.", e);
    }

    InputStream cert = getInputStream(classLoader, config.getCert(), "Server Cert");
    InputStream key = getInputStream(classLoader, config.getKey(), "Server Key");

    SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(cert, key)
        .protocols(prop.getProperty("protocols"))
        .ciphers(Arrays.asList(prop.getProperty("ciphers").split(",")));
    if (config.isMutualAuth()) {
      InputStream clientCert = getInputStream(classLoader, config.getClientCert(), "Client Cert");
      sslClientContextBuilder.trustManager(clientCert);
      sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
    }
    return GrpcSslContexts.configure(sslClientContextBuilder,
        SslProvider.OPENSSL);
  }

  private InputStream getInputStream(ClassLoader classLoader, String resource, String config) {
    InputStream is = classLoader.getResourceAsStream(resource);
    if (is == null) {
      throw new IllegalStateException("Cannot load the " + config + " from " + resource);
    }
    return is;

  }

  private OptionalInt findUnusedPort(GrpcServerConfig serverConfig) throws IOException{
    IntStream trialPorts;
    if(serverConfig.getPort()==0){
      LOG.info("No explicit port is given, system will pick up an ephemeral port.");
      if(serverConfig.isPortAutoIncrement() && serverConfig.getPortCount()>0){
        LOG.info("Port trial count must be positive: {}",serverConfig.getPortCount());
        trialPorts = IntStream.range(serverConfig.getInitialPort(),serverConfig.getInitialPort()+serverConfig.getPortCount());
      }else{
        trialPorts = IntStream.range(serverConfig.getInitialPort(),serverConfig.getInitialPort()+1);
      }
    }else{
      trialPorts = IntStream.range(serverConfig.getPort(),serverConfig.getPort()+1);
    }

    IOException[] error = new IOException[1];
    OptionalInt bindPort =  trialPorts.filter(port -> {
      try{
        ServerSocketChannel preBindServerSocketChannel = null;
        ServerSocket preBindServerSocket = null;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(serverConfig.getHost(), port);
        try {
          preBindServerSocketChannel = ServerSocketChannel.open();
          preBindServerSocket = preBindServerSocketChannel.socket();
          preBindServerSocket.setReuseAddress(true);
          preBindServerSocket.setSoTimeout((int)TimeUnit.SECONDS.toMillis(1));
          preBindServerSocket.bind(inetSocketAddress, 100);
          LOG.info("Bind successful to inet socket address {}", inetSocketAddress);
          preBindServerSocketChannel.configureBlocking(false);
          return true;
        } catch (IOException e) {
          LOG.info("Bind failed to inet socket address {}", inetSocketAddress);
          throw e;
        }finally {
          if (preBindServerSocket != null) {
            try {
              preBindServerSocket.close();
            } catch (IOException ex) {
              LOG.error("closeResource failed", ex);
            }
          }
          if(preBindServerSocketChannel != null){
            try {
              preBindServerSocketChannel.close();
            } catch (IOException ex) {
              LOG.error("closeResource failed", ex);
            }
          }
        }
      }catch (IOException e){
        error[0] = e;
      }
      return false;
    }).findAny();

    if(bindPort.isPresent()){
      return bindPort;
    }else{
      throw error[0];
    }
  }
}
