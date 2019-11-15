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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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

import com.google.common.eventbus.EventBus;
import org.apache.servicecomb.pack.alpha.core.event.GrpcStartableStartedEvent;
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
  private final EventBus eventBus;

  public GrpcStartable(GrpcServerConfig serverConfig, EventBus eventBus, BindableService... services) throws IOException {
    this.serverConfig = serverConfig;
    this.eventBus = eventBus;
    ServerBuilder<?> serverBuilder;
    try {
      OptionalInt unusedPort = findUnusedPort(serverConfig);
      if(unusedPort.isPresent()){
        serverBuilder = getServerBuilder(unusedPort.getAsInt());
        if (serverConfig.isSslEnable()){
          try {
            ((NettyServerBuilder) serverBuilder).sslContext(getSslContextBuilder(serverConfig).build());
          } catch (SSLException e) {
            throw new IllegalStateException("Unable to setup grpc to use SSL.", e);
          }
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
      eventBus.post(new GrpcStartableStartedEvent(serverConfig.getPort()));
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

  private ServerBuilder getServerBuilder(int port) {
    return NettyServerBuilder.forAddress(
        new InetSocketAddress(serverConfig.getHost(), port))
        .channelType(selectorServerChannel())
        .bossEventLoopGroup(selectorEventLoopGroup(1))
        .workerEventLoopGroup(selectorEventLoopGroup(0));
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

  /**
   * https://netty.io/wiki/native-transports.html
   *
   * RHEL/CentOS/Fedora:
   * sudo yum install autoconf automake libtool make tar \
   *                  glibc-devel libaio-devel \
   *                  libgcc.i686 glibc-devel.i686
   * Debian/Ubuntu:
   * sudo apt-get install autoconf automake libtool make tar \
   *                      gcc-multilib libaio-dev
   *
   * brew install autoconf automake libtool
   * */
  private Class<? extends ServerChannel> selectorServerChannel() {
    Class<? extends ServerChannel> channel = NioServerSocketChannel.class;
    if (serverConfig.isNativeTransport()) {
      if (OSInfo.isLinux()) {
        channel = EpollServerSocketChannel.class;
      } else if (OSInfo.isMacOS()) {
        channel = KQueueServerSocketChannel.class;
      }
    }
    LOG.info("Netty channel type is " + channel.getSimpleName());
    return channel;
  }

  private EventLoopGroup selectorEventLoopGroup(int nThreads) {
    EventLoopGroup group = new NioEventLoopGroup(nThreads);
    if (serverConfig.isNativeTransport()) {
      if (OSInfo.isLinux()) {
        group = new EpollEventLoopGroup(nThreads);
      } else if (OSInfo.isMacOS()) {
        group = new KQueueEventLoopGroup(nThreads);
      }
    }
    LOG.info("Netty event loop group is " + group.getClass().getSimpleName());
    return group;
  }

  static class OSInfo {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isLinux() {
      return OS.indexOf("linux") >= 0;
    }

    public static boolean isMacOS() {
      return OS.indexOf("mac") >= 0;
    }
  }
}
