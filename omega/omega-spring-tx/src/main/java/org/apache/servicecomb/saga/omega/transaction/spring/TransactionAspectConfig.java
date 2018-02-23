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

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.omega.context.CompensationContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.CompensationMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.SagaStartAspect;
import org.apache.servicecomb.saga.omega.transaction.TransactionAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;

@Configuration
@EnableAspectJAutoProxy
public class TransactionAspectConfig {

  @Bean
  MessageHandler messageHandler(MessageSender sender, CompensationContext context, OmegaContext omegaContext) {
    return new CompensationMessageHandler(sender, context);
  }

  @Order(0)
  @Bean
  SagaStartAspect sagaStartAspect(MessageSender sender, OmegaContext context) {
    return new SagaStartAspect(sender, context);
  }

  @Order(1)
  @Bean
  TransactionAspect transactionAspect(MessageSender sender, OmegaContext context) {
    return new TransactionAspect(sender, context);
  }

  @Bean
  CompensableAnnotationProcessor compensableAnnotationProcessor(OmegaContext omegaContext,
      CompensationContext compensationContext) {
    return new CompensableAnnotationProcessor(omegaContext, compensationContext);
  }
}
