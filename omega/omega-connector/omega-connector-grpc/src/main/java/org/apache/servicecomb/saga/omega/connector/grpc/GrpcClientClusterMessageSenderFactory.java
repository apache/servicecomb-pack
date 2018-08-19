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

import com.google.common.base.Optional;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.SSLException;
import org.apache.servicecomb.saga.omega.context.ServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

public class GrpcClientClusterMessageSenderFactory implements ClusterMessageSenderFactory {


  private ServiceConfig serviceConfig;

  public GrpcClientClusterMessageSenderFactory(ServiceConfig serviceConfig) {
    this.serviceConfig = serviceConfig;
  }

  @Override
  public List<MessageSender> apply(AlphaClusterConfig clusterConfig) {

    if (clusterConfig == null) {
      throw new IllegalArgumentException("AlphaClusterConfig must not be null");
    }

    List<String> list = Optional.fromNullable(clusterConfig.getAddresses())
        .or(Collections.<String>emptyList());

    if (list.isEmpty()) {
      throw new IllegalArgumentException("No reachable cluster address provided");
    }

    SslContext sslContext = null;

    List<MessageSender> messageSenders = new ArrayList<>();

    for (String address : clusterConfig.getAddresses()) {
      ManagedChannel channel = null;

      if (clusterConfig.isEnableSSL()) {
        if (sslContext == null) {
          try {
            sslContext = this.buildSslContext(clusterConfig);
          } catch (SSLException e) {
            throw new IllegalArgumentException("Unable to build SslContext", e);
          }
        }
        channel = NettyChannelBuilder.forTarget(address)
            .negotiationType(NegotiationType.TLS)
            .sslContext(sslContext)
            .build();
      } else {
        channel = ManagedChannelBuilder.forTarget(address).usePlaintext()
            .build();
      }

      messageSenders.add(
          new GrpcClientMessageSender(
              address,
              channel,
              clusterConfig.getMessageSerializer(),
              clusterConfig.getMessageDeserializer(),
              serviceConfig,
              clusterConfig.getMessageHandler()));
    }
    return messageSenders;
  }

  private SslContext buildSslContext(AlphaClusterConfig clusterConfig) throws SSLException {
    SslContextBuilder builder = GrpcSslContexts.forClient();
    // openssl must be used because some older JDk does not support cipher suites required by http2,
    // and the performance of JDK ssl is pretty low compared to openssl.
    builder.sslProvider(SslProvider.OPENSSL);

    Properties prop = new Properties();
    try {
      prop.load(LoadBalancedClusterMessageSender.class.getClassLoader()
          .getResourceAsStream("ssl.properties"));
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read ssl.properties.", e);
    }

    builder.protocols(prop.getProperty("protocols").split(","));
    builder.ciphers(Arrays.asList(prop.getProperty("ciphers").split(",")));
    builder.trustManager(new File(clusterConfig.getCertChain()));

    if (clusterConfig.isEnableMutualAuth()) {
      builder.keyManager(new File(clusterConfig.getCert()), new File(clusterConfig.getKey()));
    }

    return builder.build();
  }
}
