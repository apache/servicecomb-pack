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

package org.apache.servicecomb.pack.omega.transaction.spring;

import org.apache.servicecomb.pack.omega.transaction.CallbackContext;
import org.apache.servicecomb.pack.omega.context.OmegaContext;
import org.apache.servicecomb.pack.omega.transaction.CompensationMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.MessageHandler;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.SagaStartAspect;
import org.apache.servicecomb.pack.omega.transaction.TransactionAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.ParametersContext;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccParticipatorAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccStartAspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class TransactionAspectConfig {

  @Bean
  MessageHandler messageHandler(SagaMessageSender sender,
      @Qualifier("compensationContext") CallbackContext context, OmegaContext omegaContext) {
    return new CompensationMessageHandler(sender, context);
  }

  @Bean
  SagaStartAspect sagaStartAspect(SagaMessageSender sender, OmegaContext context) {
    return new SagaStartAspect(sender, context);
  }

  @Bean
  TransactionAspect transactionAspect(SagaMessageSender sender, OmegaContext context) {
    return new TransactionAspect(sender, context);
  }

  @Bean
  CompensableAnnotationProcessor compensableAnnotationProcessor(OmegaContext omegaContext,
      @Qualifier("compensationContext") CallbackContext compensationContext) {
    return new CompensableAnnotationProcessor(omegaContext, compensationContext);
  }

  @Bean
  TccMessageHandler coordinateMessageHandler(
      TccMessageSender tccMessageSender,
      @Qualifier("coordinateContext") CallbackContext coordinateContext,
      OmegaContext omegaContext,
      ParametersContext parametersContext) {
    return new CoordinateMessageHandler(tccMessageSender, coordinateContext, omegaContext, parametersContext);
  }

  @Bean
  TccStartAspect tccStartAspect(
      TccMessageSender tccMessageSender,
      OmegaContext context) {
    return new TccStartAspect(tccMessageSender, context);
  }

  @Bean
  TccParticipatorAspect tccParticipatorAspect(
      TccMessageSender tccMessageSender,
      OmegaContext context, ParametersContext parametersContext) {
    return new TccParticipatorAspect(tccMessageSender, context, parametersContext);
  }

  @Bean
  ParticipateAnnotationProcessor participateAnnotationProcessor(OmegaContext omegaContext,
      @Qualifier("coordinateContext") CallbackContext coordinateContext) {
    return new ParticipateAnnotationProcessor(omegaContext, coordinateContext);
  }
}
