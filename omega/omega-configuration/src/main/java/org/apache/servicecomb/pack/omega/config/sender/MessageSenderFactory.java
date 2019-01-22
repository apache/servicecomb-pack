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

import org.apache.servicecomb.pack.omega.config.property.AlphaClusterProperties;
import org.apache.servicecomb.pack.omega.config.property.AlphaSSLProperties;
import org.apache.servicecomb.pack.omega.config.property.OmegaClientProperties;
import org.apache.servicecomb.pack.omega.connector.grpc.AlphaClusterConfig;
import org.apache.servicecomb.pack.omega.connector.grpc.core.FastestSender;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContextFactory;
import org.apache.servicecomb.pack.omega.connector.grpc.saga.SagaLoadBalanceSender;
import org.apache.servicecomb.pack.omega.connector.grpc.tcc.TccLoadBalanceSender;
import org.apache.servicecomb.pack.omega.context.CallbackContextManager;
import org.apache.servicecomb.pack.omega.context.ParameterContextManager;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.format.KryoMessageFormat;
import org.apache.servicecomb.pack.omega.format.MessageFormat;
import org.apache.servicecomb.pack.omega.transaction.CompensationMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.MessageHandlerManager;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;

class MessageSenderFactory {

  static MessageSender newInstance(final TransactionType transactionType,
      final AlphaClusterProperties clusterProperties,
      final OmegaClientProperties clientProperties, ServiceConfig serviceConfig) {
    return createMessageSender(transactionType,
        createLoadContext(transactionType, clusterProperties, clientProperties, serviceConfig));
  }

  private static MessageSender createMessageSender(final TransactionType transactionType, final LoadBalanceContext context) {
    MessageSender result;
    switch (transactionType) {
      case SAGA:
        result = new SagaLoadBalanceSender(context, new FastestSender());
        MessageHandlerManager.register(new CompensationMessageHandler((SagaMessageSender) result,
            CallbackContextManager.getContext(transactionType)));
        break;
      case TCC:
        result = new TccLoadBalanceSender(context, new FastestSender());
        MessageHandlerManager.register(new CoordinateMessageHandler((TccMessageSender) result,
            CallbackContextManager.getContext(transactionType), ParameterContextManager.getContext(transactionType)));
        break;
      default:
        throw new UnsupportedOperationException("unsupported transaction type!");
    }
    return result;
  }

  private static LoadBalanceContext createLoadContext(final TransactionType transactionType,
      final AlphaClusterProperties clusterProperties,
      final OmegaClientProperties clientProperties,
      final ServiceConfig serviceConfig) {
    return LoadBalanceContextFactory.newInstance(
        transactionType, createAlphaClusterConfig(clusterProperties), serviceConfig,
        clientProperties.getReconnectDelayMilliSeconds(), clientProperties.getTimeoutSeconds());
  }

  private static AlphaClusterConfig createAlphaClusterConfig(final AlphaClusterProperties clusterProperties) {
    MessageFormat messageFormat = new KryoMessageFormat();
    AlphaSSLProperties ssl = clusterProperties.getSsl();
    return AlphaClusterConfig.builder()
        .addresses(clusterProperties.getAddress())
        .enableSSL(ssl.isEnableSSL())
        .enableMutualAuth(ssl.isMutualAuth())
        .cert(ssl.getCert())
        .key(ssl.getKey())
        .certChain(ssl.getCertChain())
        .messageDeserializer(messageFormat)
        .messageSerializer(messageFormat)
        .build();
  }
}
