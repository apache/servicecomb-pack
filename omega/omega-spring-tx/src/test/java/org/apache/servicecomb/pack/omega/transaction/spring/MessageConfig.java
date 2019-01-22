/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.pack.omega.transaction.spring;

import java.util.ArrayList;
import java.util.List;
import org.apache.servicecomb.pack.omega.context.CallbackContextManager;
import org.apache.servicecomb.pack.omega.context.OmegaContextManager;
import org.apache.servicecomb.pack.omega.context.ParameterContextManager;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.CompensationMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.pack.omega.transaction.SagaStartAspect;
import org.apache.servicecomb.pack.omega.transaction.TransactionAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.CoordinateMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccParticipatorAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccStartAspect;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationStartedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccStartedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageConfig {
  private final List<String> messages = new ArrayList<>();

//  @Bean
//  @SuppressWarnings("unchecked")
//  IdGenerator<String> idGenerator() {
//    return Mockito.mock(IdGenerator.class);
//  }

//  @Bean
//  OmegaContext omegaContext(IdGenerator<String> idGenerator) {
//    return new OmegaContext(idGenerator);
//  }

//  @Bean(name = "compensationContext")
//  CallbackContext recoveryCompensationContext(OmegaContext omegaContext) {
//    return new CallbackContext(omegaContext);
//  }

//  @Bean(name = "coordinateContext")
//  CallbackContext coordinateContext(OmegaContext omegaContext) {
//    return new CallbackContext(omegaContext);
//  }
//
//  @Bean
//  ParametersContext parametersContext() {
//    return new DefaultParametersContext();
//  }

  @Bean
  List<String> messages() {
    return messages;
  }

  @Bean(name = "sagaSender")
  SagaMessageSender sender() {
    return new SagaMessageSender() {
      @Override
      public void onConnected() {

      }

      @Override
      public void onDisconnected() {

      }

      @Override
      public void close() {

      }

      @Override
      public String target() {
        return "UNKNOW";
      }

      @Override
      public AlphaResponse send(Object event) {
        messages.add(event.toString());
        return new AlphaResponse(false);
      }
    };
  }

  @Bean(name = "tccSender")
  TccMessageSender TccMessageSender() {
    return new TccMessageSender() {
      @Override
      public void onConnected() {
      }

      @Override
      public void onDisconnected() {

      }

      @Override
      public AlphaResponse send(Object event) {
        return null;
      }

      @Override
      public void close() {
      }

      @Override
      public String target() {
        return "UNKNOWN";
      }

      @Override
      public AlphaResponse participationStart(ParticipationStartedEvent participationStartedEvent) {
        messages.add(participationStartedEvent.toString());
        return new AlphaResponse(false);
      }

      @Override
      public AlphaResponse participationEnd(ParticipationEndedEvent participationEndedEvent) {
        messages.add(participationEndedEvent.toString());
        return new AlphaResponse(false);
      }

      @Override
      public AlphaResponse tccTransactionStart(TccStartedEvent tccStartedEvent) {
        messages.add(tccStartedEvent.toString());
        return new AlphaResponse(false);
      }

      @Override
      public AlphaResponse tccTransactionStop(TccEndedEvent tccEndedEvent) {
        messages.add(tccEndedEvent.toString());
        return new AlphaResponse(false);
      }

      @Override
      public AlphaResponse coordinate(CoordinatedEvent coordinatedEvent) {
        messages.add(coordinatedEvent.toString());
        return new AlphaResponse(false);
      }
    };
  }

  @Bean(name = "sagaHandler")
  SagaMessageHandler messageHandler(
      @Qualifier("sagaSender") SagaMessageSender sender) {
    return new CompensationMessageHandler(sender, CallbackContextManager.getContext(TransactionType.SAGA));
  }

  @Bean(name = "tccHandler")
  TccMessageHandler coordinateMessageHandler(
      @Qualifier("tccSender") TccMessageSender tccMessageSender) {
    return new CoordinateMessageHandler(tccMessageSender, CallbackContextManager.getContext(TransactionType.TCC),
        ParameterContextManager.getContext(TransactionType.TCC));
  }

  @Bean
  SagaStartAspect sagaStartAspect(@Qualifier("sagaSender") SagaMessageSender sagaSender) {
    return new SagaStartAspect(sagaSender, OmegaContextManager.getContext());
  }

  @Bean
  TransactionAspect transactionAspect(@Qualifier("sagaSender") SagaMessageSender sagaSender) {
    return new TransactionAspect(sagaSender, OmegaContextManager.getContext());
  }

  @Bean
  TccStartAspect tccStartAspect(@Qualifier("tccSender") TccMessageSender tccSender) {
    return new TccStartAspect(tccSender, OmegaContextManager.getContext());
  }

  @Bean
  TccParticipatorAspect tccParticipatorAspect(@Qualifier("tccSender") TccMessageSender tccSender) {
    return new TccParticipatorAspect(tccSender, OmegaContextManager.getContext(), ParameterContextManager.getContext(TransactionType.TCC));
  }
}
