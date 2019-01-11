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

package org.apache.servicecomb.pack.omega.connector.grpc.tcc;

import com.google.common.base.Optional;

import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceContext;
import org.apache.servicecomb.pack.omega.connector.grpc.core.LoadBalanceSenderAdapter;
import org.apache.servicecomb.pack.omega.connector.grpc.core.MessageSenderPicker;
import org.apache.servicecomb.pack.omega.connector.grpc.core.SenderExecutor;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageSender;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationStartedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.TccStartedEvent;

public class TccLoadBalanceSender extends LoadBalanceSenderAdapter implements TccMessageSender {

  public TccLoadBalanceSender(LoadBalanceContext loadContext,
      MessageSenderPicker senderPicker) {
    super(loadContext, senderPicker);
  }

  @Override
  public AlphaResponse participationStart(ParticipationStartedEvent participationStartedEvent) {
    do {
      final TccMessageSender messageSender = pickMessageSender();
      Optional<AlphaResponse> response = doGrpcSend(messageSender, participationStartedEvent, new SenderExecutor<ParticipationStartedEvent>() {
        @Override
        public AlphaResponse apply(ParticipationStartedEvent event) {
          return messageSender.participationStart(event);
        }
      });
      if (response.isPresent()) return response.get();
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to send event " + participationStartedEvent + " due to interruption");
  }

  @Override
  public AlphaResponse participationEnd(ParticipationEndedEvent participationEndedEvent) {
    do {
      final TccMessageSender messageSender = pickMessageSender();
      Optional<AlphaResponse> response = doGrpcSend(messageSender, participationEndedEvent, new SenderExecutor<ParticipationEndedEvent>() {
        @Override
        public AlphaResponse apply(ParticipationEndedEvent event) {
          return messageSender.participationEnd(event);
        }
      });
      if (response.isPresent()) return response.get();
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to send event " + participationEndedEvent + " due to interruption");
  }

  @Override
  public AlphaResponse tccTransactionStart(TccStartedEvent tccStartEvent) {
    do {
      final TccMessageSender messageSender = pickMessageSender();
      Optional<AlphaResponse> response = doGrpcSend(messageSender, tccStartEvent, new SenderExecutor<TccStartedEvent>() {
        @Override
        public AlphaResponse apply(TccStartedEvent event) {
          return messageSender.tccTransactionStart(event);
        }
      });
      if (response.isPresent()) return response.get();
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to send event " + tccStartEvent + " due to interruption");
  }

  @Override
  public AlphaResponse tccTransactionStop(TccEndedEvent tccEndEvent) {
    do {
      final TccMessageSender messageSender = pickMessageSender();
      Optional<AlphaResponse> response = doGrpcSend(messageSender, tccEndEvent, new SenderExecutor<TccEndedEvent>() {
        @Override
        public AlphaResponse apply(TccEndedEvent event) {
          return messageSender.tccTransactionStop(event);
        }
      });
      if (response.isPresent()) return response.get();
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to send event " + tccEndEvent + " due to interruption");
  }

  @Override
  public AlphaResponse coordinate(CoordinatedEvent coordinatedEvent) {
    do {
      final TccMessageSender messageSender = pickMessageSender();
      Optional<AlphaResponse> response = doGrpcSend(messageSender, coordinatedEvent, new SenderExecutor<CoordinatedEvent>() {
        @Override
        public AlphaResponse apply(CoordinatedEvent event) {
          return messageSender.coordinate(event);
        }
      });
      if (response.isPresent()) return response.get();
    } while (!Thread.currentThread().isInterrupted());

    throw new OmegaException("Failed to send event " + coordinatedEvent + " due to interruption");
  }
}
