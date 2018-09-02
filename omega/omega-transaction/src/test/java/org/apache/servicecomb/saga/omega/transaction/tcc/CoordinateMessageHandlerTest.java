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

package org.apache.servicecomb.saga.omega.transaction.tcc;


import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.CoordinatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class CoordinateMessageHandlerTest {
  private final List<CoordinatedEvent> coordinatedEvents = new ArrayList<>();
  private final AlphaResponse response = new AlphaResponse(false);
  private final TccEventService eventService = new TccEventService() {
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
      return null;
    }

    @Override
    public AlphaResponse participate(ParticipatedEvent participateEvent) {
      return null;
    }

    @Override
    public AlphaResponse tccTransactionStart(TccStartedEvent tccStartEvent) {
      return null;
    }

    @Override
    public AlphaResponse tccTransactionStop(TccEndedEvent tccEndEvent) {
      return null;
    }

    @Override
    public AlphaResponse coordinate(CoordinatedEvent coordinatedEvent) {
      coordinatedEvents.add(coordinatedEvent);
      return response;
    }
  };

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String methodName= uniquify("Method");

  private final CallbackContext callbackContext = Mockito.mock(CallbackContext.class);
  private final OmegaContext omegaContext = Mockito.mock(OmegaContext.class);
  private final ParametersContext parametersContext = Mockito.mock(ParametersContext.class);
  private final CoordinateMessageHandler handler = new CoordinateMessageHandler(eventService, callbackContext, omegaContext, parametersContext);

  @Before
  public void setUp() {
    coordinatedEvents.clear();
  }

  @Test
  public void sendsCompensatedEventOnCompensationCompleted() {
    handler.onReceive(globalTxId, localTxId, parentTxId, methodName);

    assertThat(coordinatedEvents.size(), is(1));

    CoordinatedEvent event = coordinatedEvents.get(0);
    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(localTxId));
    assertThat(event.getParentTxId(), is(parentTxId));
    assertThat(event.getMethodName(), is(methodName));
    assertThat(event.getStatus(), is(TransactionStatus.Succeed));
  }

}
