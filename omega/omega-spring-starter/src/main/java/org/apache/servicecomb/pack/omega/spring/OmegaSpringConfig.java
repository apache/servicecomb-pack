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

package org.apache.servicecomb.pack.omega.spring;

import org.apache.servicecomb.pack.omega.config.sender.MessageSenderManager;
import org.apache.servicecomb.pack.omega.context.OmegaContextManager;
import org.apache.servicecomb.pack.omega.context.ParameterContextManager;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.spring.properties.BootAlphaClusterProperties;
import org.apache.servicecomb.pack.omega.spring.properties.BootOmegaClientProperties;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.SagaStartAspect;
import org.apache.servicecomb.pack.omega.transaction.TransactionAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccParticipatorAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccStartAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableConfigurationProperties({
    BootAlphaClusterProperties.class, BootOmegaClientProperties.class
})
@EnableAspectJAutoProxy
class OmegaSpringConfig {

  public OmegaSpringConfig(
      BootAlphaClusterProperties alphaClusterProperties, BootOmegaClientProperties omegaClientProperties,
      @Value("${spring.application.name}") String serviceName, @Value("${omega.instance.instanceId:#{null}}") String instanceId) {
    MessageSenderManager.register(alphaClusterProperties, omegaClientProperties, new ServiceConfig(serviceName, instanceId));
  }

  @Bean
  SagaStartAspect sagaStartAspect() {
    return new SagaStartAspect((SagaMessageSender) MessageSenderManager.getMessageSender(TransactionType.SAGA),
        OmegaContextManager.getContext());
  }

  @Bean
  TransactionAspect transactionAspect() {
    return new TransactionAspect((SagaMessageSender) MessageSenderManager.getMessageSender(TransactionType.SAGA),
        OmegaContextManager.getContext());
  }

  @Bean
  TccStartAspect tccStartAspect() {
    return new TccStartAspect((TccMessageSender) MessageSenderManager.getMessageSender(TransactionType.TCC),
        OmegaContextManager.getContext());
  }

  @Bean
  TccParticipatorAspect tccParticipatorAspect() {
    return new TccParticipatorAspect((TccMessageSender) MessageSenderManager.getMessageSender(TransactionType.TCC),
        OmegaContextManager.getContext(), ParameterContextManager.getContext(TransactionType.TCC));
  }
}
