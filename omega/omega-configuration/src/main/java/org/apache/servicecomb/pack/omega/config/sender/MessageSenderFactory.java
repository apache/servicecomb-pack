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
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;

public class MessageSenderFactory {

  private static final Map<TransactionType, MessageSender> SENDERS_MAP = new ConcurrentHashMap<>();

//  public static MessageSender newInstance(final AlphaClusterProperties alphaClusterProperties,
//      final OmegaClientProperties omegaClientProperties, ServiceConfig serviceConfig) {
//
//  }

//  public MessageSenderFactory() {
//
//    AlphaSSLProperties ssl = alphaClusterProperties.getSsl();
//    MessageFormat messageFormat = new KryoMessageFormat();
//    AlphaClusterConfig clusterConfig = AlphaClusterConfig.builder()
//        .addresses(alphaClusterProperties.getAddress())
//        .enableSSL(ssl.isEnableSSL())
//        .enableMutualAuth(ssl.isMutualAuth())
//        .cert(ssl.getCert())
//        .key(ssl.getKey())
//        .certChain(ssl.getCertChain())
//        .messageDeserializer(messageFormat)
//        .messageSerializer(messageFormat)
//        .messageHandler(messageHandler)
//        .tccMessageHandler(tccMessageHandler)
//        .build();
//  }
//
//  static {
//
//  }



  public static MessageSender getMessageSender(final TransactionType transactionType) {
    return SENDERS_MAP.get(transactionType);
  }
}
