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

package org.apache.servicecomb.pack.alpha.spec.saga.db;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.singletonList;
import static org.apache.servicecomb.pack.common.EventType.SagaEndedEvent;
import static org.apache.servicecomb.pack.common.EventType.SagaStartedEvent;
import static org.apache.servicecomb.pack.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.pack.common.EventType.TxCompensatedEvent;
import static org.apache.servicecomb.pack.common.EventType.TxEndedEvent;
import static org.apache.servicecomb.pack.common.EventType.TxStartedEvent;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {WebConfiguration.class})
@WebMvcTest(SagaTransactionsController.class)
public class SagaTransactionsControllerTest {
  private final TxEvent someEvent = populateEvents(TxStartedEvent.name());

  List<TxEvent> eventStarted, eventCompensated, eventCommitted, committedTransactions, compensatingTransaction, pendingTransactions, rollbackedTransactions, allTransactions;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TxEventEnvelopeRepository eventRepository;

  @Before
  public void setUp() throws Exception {
    when(eventRepository.findAll()).thenReturn(singletonList(someEvent));
    when(eventRepository.findTotalCountOfTransactions()).thenReturn(10);
    when(eventRepository.findCountOfCommittedEvents()).thenReturn(5);
    when(eventRepository.findCountOfPendingEvents()).thenReturn(1);
    when(eventRepository.findCountOfRollBackedEvents()).thenReturn(2);
    when(eventRepository.findCountOfCompensatingEvents()).thenReturn(2);

    // Populate events for /recent API's
    eventStarted = new LinkedList<>();
    eventStarted.add(populateEvents(TxStartedEvent.name()));
    when(eventRepository.findPendingEvents(PageRequest.of(0, 5))).thenReturn(eventStarted);
    when(eventRepository.findCompensatingEvents(PageRequest.of(0, 5))).thenReturn(eventStarted);

    eventCompensated = new LinkedList<>();
    eventCompensated.add(populateEvents(TxCompensatedEvent.name()));
    when(eventRepository.findRollBackedEvents(PageRequest.of(0, 5))).thenReturn(eventCompensated);

    eventCommitted = new LinkedList<>();
    eventCommitted.add(populateEvents(TxEndedEvent.name()));
    eventCommitted.add(populateEvents(SagaEndedEvent.name()));
    when(eventRepository.findCommittedEvents(PageRequest.of(0, 5))).thenReturn(eventCommitted);

    // Populate events for /transactions
    pendingTransactions = new LinkedList<>();
    pendingTransactions.add(populateEvents(SagaStartedEvent.name()));
    pendingTransactions.add(populateEvents(TxStartedEvent.name()));
    when(eventRepository.findPendingEvents()).thenReturn(pendingTransactions);

    compensatingTransaction = new LinkedList<>();
    compensatingTransaction.add(populateEvents(SagaStartedEvent.name()));
    compensatingTransaction.add(populateEvents(TxStartedEvent.name()));
    compensatingTransaction.add(populateEvents(TxAbortedEvent.name()));
    when(eventRepository.findCompensatingEvents()).thenReturn(compensatingTransaction);

    rollbackedTransactions = new LinkedList<>();
    rollbackedTransactions.add(populateEvents(SagaStartedEvent.name()));
    rollbackedTransactions.add(populateEvents(TxStartedEvent.name()));
    rollbackedTransactions.add(populateEvents(TxAbortedEvent.name()));
    rollbackedTransactions.add(populateEvents(TxCompensatedEvent.name()));
    rollbackedTransactions.add(populateEvents(TxEndedEvent.name()));
    when(eventRepository.findRollBackedEvents()).thenReturn(rollbackedTransactions);

    committedTransactions = new LinkedList<>();
    committedTransactions.add(populateEvents(SagaStartedEvent.name()));
    committedTransactions.add(populateEvents(TxStartedEvent.name()));
    committedTransactions.add(populateEvents(TxEndedEvent.name()));
    committedTransactions.add(populateEvents(SagaEndedEvent.name()));
    when(eventRepository.findCommittedEvents()).thenReturn(committedTransactions);

    // Populate events for /findTransactions
    when(eventRepository.findByGlobalTxId("XXXGID")).thenReturn(committedTransactions);
    when(eventRepository.findByServiceName("XXService")).thenReturn(singletonList(someEvent));
  }

  @Test
  public void getDashboardStats() throws Exception {
    mockMvc.perform(get("/saga/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalTransactions", is(10)))
        .andExpect(jsonPath("$.committedTransactions", is(5)))
        .andExpect(jsonPath("$.pendingTransactions", is(1)))
        .andExpect(jsonPath("$.compensatingTransactions", is(2)))
        .andExpect(jsonPath("$.failureRate", is(40)))
        .andExpect(jsonPath("$.rollbackTransactions", is(2)));
  }


  @Test
  public void getRecentTransactionsTest() throws Exception {
    mockMvc.perform(get("/saga/recent?status=PENDING&count=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc.perform(get("/saga/recent?status=COMMITTED&count=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
    mockMvc.perform(get("/saga/recent?status=COMPENSATING&count=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc.perform(get("/saga/recent?status=ROLLBACKED&count=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc.perform(get("/saga/recent?status=FAILURECondition&count=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  public void getTransactionsTest() throws Exception {
    mockMvc.perform(get("/saga/transactions?status=PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
    mockMvc.perform(get("/saga/transactions?status=COMMITTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)));
    mockMvc.perform(get("/saga/transactions?status=COMPENSATING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)));
    mockMvc.perform(get("/saga/transactions?status=ROLLBACKED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)));
    mockMvc.perform(get("/saga/transactions?status=ALL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc.perform(get("/saga/transactions?status=FAILURECondition"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  public void findTransactionsTest() throws Exception {
    mockMvc.perform(get("/saga/findTransactions?globalTxID=XXXGID"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)));
    mockMvc.perform(get("/saga/findTransactions?microServiceName=XXService"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc.perform(get("/saga/findTransactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  private TxEvent populateEvents(String type) {
    return new TxEvent(
        uniquify("serviceName"),
        uniquify("instanceId"),
        uniquify("globalTxId"),
        uniquify("localTxId"),
        UUID.randomUUID().toString(),
        type,
        this.getClass().getCanonicalName(),
        uniquify("blah").getBytes());
  }
}
