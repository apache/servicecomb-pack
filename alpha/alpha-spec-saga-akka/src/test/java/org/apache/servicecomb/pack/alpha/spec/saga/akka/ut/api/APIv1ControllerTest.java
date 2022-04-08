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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.ut.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.NodeStatus.TypeEnum;
import org.apache.servicecomb.pack.alpha.core.fsm.TransactionType;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.PagingGlobalTransactions;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.SagaSubTransaction;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.apache.servicecomb.pack.alpha.core.metrics.MetricsBean;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.api.SagaAkkaAPIv1Controller;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.SagaActorState;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.repository.TransactionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
@WebMvcTest(SagaAkkaAPIv1Controller.class)
public class APIv1ControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  AlphaMetricsEndpoint alphaMetricsEndpoint;

  @Autowired
  MetricsService metricsService;

  @Autowired
  NodeStatus nodeStatus;

  @Autowired
  ElasticsearchRestTemplate template;

  @Autowired
  TransactionRepository transactionRepository;

  @Test
  public void metricsTest() throws Exception {
    MetricsBean metricsBean = new MetricsBean();
    metricsBean.doEventReceived();
    metricsBean.doEventAccepted();
    metricsBean.doEventAvgTime(5);
    metricsBean.doActorReceived();
    metricsBean.doActorAccepted();
    metricsBean.doActorAvgTime(5);
    metricsBean.doRepositoryReceived();
    metricsBean.doRepositoryAccepted();
    metricsBean.doRepositoryAvgTime(5);
    metricsBean.doCommitted();
    metricsBean.doCompensated();
    metricsBean.doSuspended();
    metricsBean.doSagaBeginCounter();
    metricsBean.doSagaEndCounter();
    metricsBean.doSagaAvgTime(5);
    when(metricsService.metrics()).thenReturn(metricsBean);
    when(nodeStatus.getTypeEnum()).thenReturn(TypeEnum.MASTER);
    mockMvc.perform(get("/alpha/api/v1/metrics"))
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.metrics.eventReceived").value(1))
        .andExpect(jsonPath("$.metrics.eventAccepted").value(1))
        .andExpect(jsonPath("$.metrics.eventRejected").value(0))
        .andExpect(jsonPath("$.metrics.eventAvgTime").value(5.0))
        .andExpect(jsonPath("$.metrics.actorReceived").value(1))
        .andExpect(jsonPath("$.metrics.actorAccepted").value(1))
        .andExpect(jsonPath("$.metrics.actorRejected").value(0))
        .andExpect(jsonPath("$.metrics.actorAvgTime").value(5.0))
        .andExpect(jsonPath("$.metrics.repositoryReceived").value(1))
        .andExpect(jsonPath("$.metrics.repositoryAccepted").value(1))
        .andExpect(jsonPath("$.metrics.repositoryRejected").value(0))
        .andExpect(jsonPath("$.metrics.repositoryAvgTime").value(5.0))
        .andExpect(jsonPath("$.metrics.sagaBeginCounter").value(1))
        .andExpect(jsonPath("$.metrics.sagaEndCounter").value(1))
        .andExpect(jsonPath("$.metrics.sagaAvgTime").value(5.0))
        .andExpect(jsonPath("$.metrics.committed").value(1))
        .andExpect(jsonPath("$.metrics.compensated").value(1))
        .andExpect(jsonPath("$.metrics.suspended").value(1))
        .andExpect(jsonPath("$.nodeType").value(TypeEnum.MASTER.name()))
        .andReturn();
  }

  @Test
  public void transactionTest() throws Exception {
    final String serviceName = "serviceName-1";
    final String instanceId = "instanceId-1";
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();

    List<BaseEvent> events = new ArrayList();
    events.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g")
        .globalTxId(globalTxId).build());
    events.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    events.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    events.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    events.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    events.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    events.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    events.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g")
        .globalTxId(globalTxId).build());

    List<SagaSubTransaction> subTransactions = new ArrayList();
    subTransactions
        .add(SagaSubTransaction.builder().parentTxId(globalTxId).localTxId(localTxId_1).state(
            TxState.COMMITTED).beginTime(new Date()).endTime(new Date()).build());
    subTransactions
        .add(SagaSubTransaction.builder().parentTxId(globalTxId).localTxId(localTxId_2).state(
            TxState.COMMITTED).beginTime(new Date()).endTime(new Date()).build());
    subTransactions
        .add(SagaSubTransaction.builder().parentTxId(globalTxId).localTxId(localTxId_3).state(
            TxState.COMMITTED).beginTime(new Date()).endTime(new Date()).build());

    List<GlobalTransaction> globalTransactions = new ArrayList<>();
    globalTransactions.add(GlobalTransaction.builder()
        .serviceName(serviceName)
        .instanceId(instanceId)
        .globalTxId(globalTxId)
        .type(TransactionType.SAGA)
        .state(SagaActorState.COMMITTED.name())
        .beginTime(new Date())
        .endTime(new Date())
        .subTxSize(3)
        .events(events)
        .subTransactions(subTransactions)
        .build());
    PagingGlobalTransactions paging = PagingGlobalTransactions.builder()
        .page(0)
        .size(50)
        .elapsed(10)
        .total(1)
        .globalTransactions(globalTransactions)
        .build();

    when(transactionRepository.getGlobalTransactions(null, 0, 50)).thenReturn(paging);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
    mapper.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, false);
    mockMvc.perform(get("/alpha/api/v1/transaction?page=0&size=50"))
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(50))
        .andExpect(jsonPath("$.elapsed").value(10))
        .andExpect(jsonPath("$.globalTransactions", hasSize(1)))
        .andExpect(jsonPath("$.globalTransactions[0].globalTxId")
            .value(globalTransactions.get(0).getGlobalTxId()))
        .andExpect(jsonPath("$.globalTransactions[0].type")
            .value(globalTransactions.get(0).getType().name()))
        .andExpect(jsonPath("$.globalTransactions[0].serviceName")
            .value(globalTransactions.get(0).getServiceName()))
        .andExpect(jsonPath("$.globalTransactions[0].instanceId")
            .value(globalTransactions.get(0).getInstanceId()))
        .andExpect(jsonPath("$.globalTransactions[0].beginTime")
            .value(globalTransactions.get(0).getBeginTime().getTime()))
        .andExpect(jsonPath("$.globalTransactions[0].endTime")
            .value(globalTransactions.get(0).getEndTime().getTime()))
        .andExpect(jsonPath("$.globalTransactions[0].state")
            .value(globalTransactions.get(0).getState()))
        .andExpect(jsonPath("$.globalTransactions[0].subTxSize")
            .value(globalTransactions.get(0).getSubTxSize()))
        .andExpect(jsonPath("$.globalTransactions[0].durationTime")
            .value(globalTransactions.get(0).getDurationTime()))
        .andExpect(jsonPath("$.globalTransactions[0].subTransactions", hasSize(3)))
        .andExpect(jsonPath("$.globalTransactions[0].events", hasSize(8)))
        .andReturn();
  }

  @Test
  public void globalTransactionByGlobalTxIdTest() throws Exception {
    final String serviceName = "serviceName-1";
    final String instanceId = "instanceId-1";
    final String globalTxId = UUID.randomUUID().toString();
    final String localTxId_1 = UUID.randomUUID().toString();
    final String localTxId_2 = UUID.randomUUID().toString();
    final String localTxId_3 = UUID.randomUUID().toString();

    List<BaseEvent> events = new ArrayList();
    events.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g")
        .globalTxId(globalTxId).build());
    events.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    events.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
    events.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    events.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
    events.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    events.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3")
        .globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
    events.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g")
        .globalTxId(globalTxId).build());

    List<SagaSubTransaction> subTransactions = new ArrayList();
    subTransactions
        .add(SagaSubTransaction.builder().parentTxId(globalTxId).localTxId(localTxId_1).state(
            TxState.COMMITTED).beginTime(new Date()).endTime(new Date()).build());
    subTransactions
        .add(SagaSubTransaction.builder().parentTxId(globalTxId).localTxId(localTxId_2).state(
            TxState.COMMITTED).beginTime(new Date()).endTime(new Date()).build());
    subTransactions
        .add(SagaSubTransaction.builder().parentTxId(globalTxId).localTxId(localTxId_3).state(
            TxState.COMMITTED).beginTime(new Date()).endTime(new Date()).build());

    GlobalTransaction globalTransaction = GlobalTransaction.builder()
        .serviceName(serviceName)
        .instanceId(instanceId)
        .globalTxId(globalTxId)
        .type(TransactionType.SAGA)
        .state(SagaActorState.COMMITTED.name())
        .beginTime(new Date())
        .endTime(new Date())
        .subTxSize(3)
        .events(events)
        .subTransactions(subTransactions)
        .build();

    when(transactionRepository.getGlobalTransactionByGlobalTxId(globalTxId)).thenReturn(
        globalTransaction);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
    mapper.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, false);
    mockMvc.perform(get("/alpha/api/v1/transaction/" + globalTxId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.globalTxId")
            .value(globalTransaction.getGlobalTxId()))
        .andExpect(jsonPath("$.type")
            .value(globalTransaction.getType().name()))
        .andExpect(jsonPath("$.serviceName")
            .value(globalTransaction.getServiceName()))
        .andExpect(jsonPath("$.instanceId")
            .value(globalTransaction.getInstanceId()))
        .andExpect(jsonPath("$.beginTime")
            .value(globalTransaction.getBeginTime().getTime()))
        .andExpect(jsonPath("$.endTime")
            .value(globalTransaction.getEndTime().getTime()))
        .andExpect(jsonPath("$.state")
            .value(globalTransaction.getState()))
        .andExpect(jsonPath("$.subTxSize")
            .value(globalTransaction.getSubTxSize()))
        .andExpect(jsonPath("$.durationTime")
            .value(globalTransaction.getDurationTime()))
        .andExpect(jsonPath("$.subTransactions", hasSize(3)))
        .andExpect(jsonPath("$.events", hasSize(8)))
        .andReturn();
  }

  @Test
  public void transactionStatisticsTest() throws Exception {
    Map<String, Long> statistics = new HashMap<>();
    statistics.put("COMMITTED", 1l);
    statistics.put("SUSPENDED", 2l);
    statistics.put("COMPENSATED", 3l);
    when(transactionRepository.getTransactionStatistics()).thenReturn(statistics);
    mockMvc.perform(get("/alpha/api/v1/transaction/statistics"))
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.COMMITTED").value(statistics.get("COMMITTED")))
        .andExpect(jsonPath("$.SUSPENDED").value(statistics.get("SUSPENDED")))
        .andExpect(jsonPath("$.COMPENSATED").value(statistics.get("COMPENSATED")))
        .andReturn();
  }

  @Test
  public void transactionSlowTest() throws Exception {
    List<GlobalTransaction> globalTransactions = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      globalTransactions.add(GlobalTransaction.builder()
          .beginTime(new Date())
          .endTime(new Date())
          .events(new ArrayList<>())
          .subTransactions(new ArrayList<>())
          .build());
    }
    when(transactionRepository.getSlowGlobalTransactionsTopN(10)).thenReturn(globalTransactions);
    mockMvc.perform(get("/alpha/api/v1/transaction/slow"))
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(10)))
        .andReturn();
  }
}
