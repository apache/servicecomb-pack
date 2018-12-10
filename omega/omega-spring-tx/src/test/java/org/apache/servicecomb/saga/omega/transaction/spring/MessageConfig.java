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

package org.apache.servicecomb.saga.omega.transaction.spring;

import java.util.ArrayList;
import java.util.List;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.context.IdGenerator;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.SagaMessageSender;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.DefaultParametersContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.ParametersContext;
import org.apache.servicecomb.saga.omega.transaction.tcc.TccMessageSender;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageConfig {
  private final List<String> messages = new ArrayList<>();

  @Bean
  IdGenerator<String> idGenerator() {
    return Mockito.mock(IdGenerator.class);
  }

  @Bean(name = "compensationContext")
  CallbackContext recoveryCompensationContext(OmegaContext omegaContext) {
    return new CallbackContext(omegaContext);
  }

  @Bean(name = "coordinateContext")
  CallbackContext coordinateContext(OmegaContext omegaContext) {
    return new CallbackContext(omegaContext);
  }

  @Bean
  ParametersContext parametersContext() {
    return new DefaultParametersContext();
  }

  @Bean
  OmegaContext omegaContext(IdGenerator<String> idGenerator) {
    return new OmegaContext(idGenerator);
  }

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
      public AlphaResponse send(TxEvent event) {
        messages.add(event.toString());
        return new AlphaResponse(false);
      }
    };
  }

  @Bean
  TccMessageSender TccMessageSender() {
    return new TccMessageSender() {
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
        return "UNKNOWN";
      }

      @Override
      public AlphaResponse participate(ParticipatedEvent participatedEvent) {
        messages.add(participatedEvent.toString());
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
}
