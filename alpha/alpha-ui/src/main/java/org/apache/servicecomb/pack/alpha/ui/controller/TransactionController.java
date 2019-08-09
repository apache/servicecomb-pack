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

package org.apache.servicecomb.pack.alpha.ui.controller;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.core.api.APIv1;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.PagingGlobalTransactions;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetrics;
import org.apache.servicecomb.pack.alpha.ui.vo.DataTablesRequestDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.DataTablesResponseDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.EventDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.SubTransactionDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.TransactionRowDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.TransactionStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.misc.BASE64Decoder;

@Controller
@EnableScheduling
public class TransactionController {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String WEBSOCKET_BROKER_METRICES_TOPIC = "/topic/metrics";

  @Autowired
  SimpMessagingTemplate template;

  @Autowired
  APIv1 apiv1;

  boolean applicationReady = false;

  @PostMapping("/ui/transaction/sagalist")
  @ResponseBody
  public DataTablesResponseDTO sagaList(@ModelAttribute DataTablesRequestDTO dataTablesRequestDTO)
      throws Exception {
    List<TransactionRowDTO> data = new ArrayList<>();
    PagingGlobalTransactions pagingGlobalTransactions = apiv1
        .getTransactions(dataTablesRequestDTO.getState(),
            dataTablesRequestDTO.getStart() / dataTablesRequestDTO.getLength(),
            dataTablesRequestDTO.getLength());
    pagingGlobalTransactions.getGlobalTransactions().forEach(globalTransaction -> {
      data.add(TransactionRowDTO.builder()
          .serviceName(globalTransaction.getServiceName())
          .instanceId(globalTransaction.getInstanceId())
          .globalTxId(globalTransaction.getGlobalTxId())
          .state(globalTransaction.getState())
          .beginTime(globalTransaction.getBeginTime())
          .endTime(globalTransaction.getEndTime())
          .subTxSize(globalTransaction.getSubTxSize())
          .durationTime(globalTransaction.getDurationTime())
          .build());
    });
    return DataTablesResponseDTO.builder()
        .draw(dataTablesRequestDTO.getDraw())
        .recordsTotal(pagingGlobalTransactions.getTotal())
        .recordsFiltered(pagingGlobalTransactions.getTotal())
        .data(data)
        .build();
  }

  // TODO The state machine is not yet supported
  @PostMapping("/ui/transaction/tcclist")
  @ResponseBody
  public DataTablesResponseDTO tccList(@ModelAttribute DataTablesRequestDTO dataTablesRequestDTO) {
    List<TransactionRowDTO> data = new ArrayList<>();
    return DataTablesResponseDTO.builder()
        .draw(dataTablesRequestDTO.getDraw())
        .recordsTotal(0)
        .recordsFiltered(0)
        .data(data)
        .build();
  }

  @PostMapping("/ui/transaction/search")
  @ResponseBody
  public DataTablesResponseDTO searchList(
      @ModelAttribute DataTablesRequestDTO dataTablesRequestDTO) throws Exception {
    if (dataTablesRequestDTO.getQuery() != null) {
      List<TransactionRowDTO> data = new ArrayList<>();
      GlobalTransaction globalTransaction = findGlobalTransactionByGlobalTxId(
          dataTablesRequestDTO.getQuery());
      if (globalTransaction != null) {
        data.add(TransactionRowDTO.builder()
            .serviceName(globalTransaction.getServiceName())
            .instanceId(globalTransaction.getInstanceId())
            .globalTxId(globalTransaction.getGlobalTxId())
            .state(globalTransaction.getState())
            .beginTime(globalTransaction.getBeginTime())
            .endTime(globalTransaction.getEndTime())
            .subTxSize(globalTransaction.getSubTxSize())
            .durationTime(globalTransaction.getDurationTime())
            .build());
      }
      return DataTablesResponseDTO.builder()
          .draw(dataTablesRequestDTO.getDraw())
          .recordsTotal(1)
          .recordsFiltered(1)
          .data(data)
          .build();
    } else {
      return this.sagaList(dataTablesRequestDTO);
    }
  }

  @GetMapping("/ui/transaction/{globalTxId}")
  public String getGlobalTransaction(ModelMap map, @PathVariable("globalTxId") String globalTxId)
      throws Exception {
    List<EventDTO> events = new ArrayList<>();
    List<SubTransactionDTO> subTransactions = new ArrayList<>();
    GlobalTransaction globalTransaction = findGlobalTransactionByGlobalTxId(globalTxId);
    globalTransaction.getEvents().forEach(event -> {
      EventDTO eventDTO = EventDTO.builder()
          // Common Event properties
          .type(event.get("type").toString())
          .serviceName(event.get("serviceName").toString())
          .instanceId(event.get("instanceId").toString())
          .timeout(event.get("timeout") != null ? Integer.valueOf(event.get("timeout").toString()) : 0)
          .globalTxId(event.get("globalTxId").toString())
          .parentTxId(event.get("parentTxId") != null ? event.get("parentTxId").toString() : null)
          .localTxId(event.get("localTxId") != null ? event.get("localTxId").toString() : null)
          .createTime(new Date(Long.valueOf(event.get("createTime").toString())))
          .build();
      if (eventDTO.getType().equals("TxStartedEvent")) {
        // TxStartedEvent properties
        if (event.containsKey("compensationMethod")) {
          eventDTO.setCompensationMethod(event.get("compensationMethod").toString());
        }
        if (event.containsKey("retries")) {
          eventDTO.setRetries(Long.valueOf(event.get("retries").toString()));
        }
        if (event.containsKey("timeout")) {
          eventDTO.setTimeout(Long.valueOf(event.get("timeout").toString()));
        }
      }
      if (eventDTO.getType().equals("TxAbortedEvent") || eventDTO.getType()
          .equals("SagaAbortedEvent")) {
        // TxAbortedEvent properties
        if (event.containsKey("payloads")) {
          BASE64Decoder decoder = new BASE64Decoder();
          String exception;
          try {
            exception = new String(decoder.decodeBuffer(event.get("payloads").toString()), "UTF-8");
          } catch (IOException e) {
            exception = "BASE64Decoder error";
            LOG.error(e.getMessage(), e);
          }
          eventDTO.setException(exception);
        }
      }
      events.add(eventDTO);
    });

    globalTransaction.getSubTransactions().forEach(sub -> {
      subTransactions.add(
          SubTransactionDTO.builder().parentTxId(globalTxId).localTxId(sub.getLocalTxId())
              .beginTime(sub.getBeginTime()).endTime(sub.getEndTime())
              .durationTime(sub.getDurationTime()).state(sub.getState().name()).build());
    });
    map.put("events", events);
    map.put("globalTxId", globalTransaction.getGlobalTxId());
    map.put("state", globalTransaction.getState());
    map.put("endTime", globalTransaction.getEndTime());
    map.put("suspendedType", globalTransaction.getSuspendedType());
    map.put("subTransactions", subTransactions);
    return "transaction_details";
  }

  @GetMapping("/ui/transaction/statistics")
  @ResponseBody
  public TransactionStatisticsDTO getGlobalTransactionStatistics() {
    TransactionStatisticsDTO statisticsDTO = new TransactionStatisticsDTO();
    Map<String, Long> statistics = apiv1.getTransactionStatistics();
    if (statistics.containsKey("COMMITTED")) {
      statisticsDTO.setSuccessful(statistics.get("COMMITTED").longValue());
    }
    if (statistics.containsKey("SUSPENDED")) {
      statisticsDTO.setFailed(statistics.get("SUSPENDED").longValue());
    }
    if (statistics.containsKey("COMPENSATED")) {
      statisticsDTO.setCompensated(statistics.get("COMPENSATED").longValue());
    }
    return statisticsDTO;
  }

  @GetMapping("/ui/transaction/slow")
  @ResponseBody
  public List<TransactionRowDTO> getSlowGlobalTransactionTopN() {
    List<TransactionRowDTO> transactionRowDTOS = new ArrayList<>();
    List<GlobalTransaction> transactions = apiv1.getSlowTransactions();
    transactions.stream().forEach(globalTransaction -> {
      transactionRowDTOS.add(TransactionRowDTO.builder()
          .serviceName(globalTransaction.getServiceName())
          .instanceId(globalTransaction.getInstanceId())
          .globalTxId(globalTransaction.getGlobalTxId())
          .state(globalTransaction.getState())
          .beginTime(globalTransaction.getBeginTime())
          .endTime(globalTransaction.getEndTime())
          .subTxSize(globalTransaction.getSubTxSize())
          .durationTime(globalTransaction.getDurationTime())
          .build());
    });
    return transactionRowDTOS;
  }

  @GetMapping("/ui/transaction/metrics")
  @ResponseBody
  public AlphaMetrics getMetrics() {
    return getAlphaMetrics();
  }

  @Scheduled(fixedDelay = 1000)
  public void publishUpdates() {
    if (applicationReady) {
      template.convertAndSend(WEBSOCKET_BROKER_METRICES_TOPIC, getAlphaMetrics());
    }
  }

  private GlobalTransaction findGlobalTransactionByGlobalTxId(String globalTxId) throws Exception {
    GlobalTransaction globalTransaction = apiv1.getTransactionByGlobalTxId(globalTxId);
    return globalTransaction;
  }

  private AlphaMetrics getAlphaMetrics() {
    AlphaMetrics alphaMetrics = apiv1.getMetrics();
    return alphaMetrics;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startUp() {
    applicationReady = true;
  }
}
