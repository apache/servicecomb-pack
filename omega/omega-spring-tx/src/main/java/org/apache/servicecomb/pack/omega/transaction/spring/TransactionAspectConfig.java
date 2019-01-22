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

import org.apache.servicecomb.pack.omega.context.CallbackContextManager;
import org.apache.servicecomb.pack.omega.context.OmegaContextManager;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class TransactionAspectConfig {

//  @Bean
//  SagaMessageHandler messageHandler(SagaMessageSender sender,
//      @Qualifier("compensationContext") CallbackContext context, OmegaContext omegaContext) {
//    return new CompensationMessageHandler(sender, context);
//  }

//  @Bean
//  SagaStartAspect sagaStartAspect() {
//    return new SagaStartAspect(, context);
//  }

//  @Bean
//  TransactionAspect transactionAspect(SagaMessageSender sender, OmegaContext context) {
//    return new TransactionAspect(sender, context);
//  }

  @Bean
  CompensableAnnotationProcessor compensableAnnotationProcessor() {
    return new CompensableAnnotationProcessor(OmegaContextManager.getContext(),
        CallbackContextManager.getContext(TransactionType.SAGA));
  }


  @Bean
  ParticipateAnnotationProcessor participateAnnotationProcessor() {
    return new ParticipateAnnotationProcessor(OmegaContextManager.getContext(),
        CallbackContextManager.getContext(TransactionType.TCC));
  }
}
