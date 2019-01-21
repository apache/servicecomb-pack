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

package org.apache.servicecomb.pack.omega.config.sender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.TransactionType;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.format.KryoMessageFormat;
import org.apache.servicecomb.pack.omega.format.MessageFormat;
import org.apache.servicecomb.pack.omega.properties.AlphaClusterProperties;
import org.apache.servicecomb.pack.omega.properties.AlphaSSLProperties;
import org.apache.servicecomb.pack.omega.properties.OmegaClientProperties;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;

public class MessageSenderRegistry {

  private static final Map<TransactionType, MessageSender> SENDERS_MAP = new ConcurrentHashMap<>();

  private AlphaClusterProperties alphaClusterProperties;

  private OmegaClientProperties omegaClientProperties;

  private ServiceConfig serviceConfig;

  private AlphaClusterConfig alphaClusterConfig;

  private MessageHandler messageHandler;

  private TccMessageHandler tccMessageHandler;

  public MessageSenderRegistry(AlphaClusterProperties alphaClusterProperties,
      OmegaClientProperties omegaClientProperties, ServiceConfig serviceConfig,
      AlphaClusterConfig alphaClusterConfig) {


    this.alphaClusterProperties = alphaClusterProperties;
    this.omegaClientProperties = omegaClientProperties;
    this.serviceConfig = serviceConfig;
    this.alphaClusterConfig = alphaClusterConfig;

    AlphaSSLProperties ssl = alphaClusterProperties.getSsl();
    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
        .addresses(alphaClusterProperties.getAddress())
        .enableSSL(ssl.isEnableSSL())
        .enableMutualAuth(ssl.isMutualAuth())
        .cert(ssl.getCert())
        .key(ssl.getKey())
        .certChain(ssl.getCertChain())
        .messageDeserializer(messageFormat)
        .messageSerializer(messageFormat)
        .messageHandler(messageHandler)
        .tccMessageHandler(tccMessageHandler)
        .build();
  }

  static {

  }

  public static MessageSender getMessageSender(final TransactionType transactionType) {
    return SENDERS_MAP.get(transactionType);
  }
}
