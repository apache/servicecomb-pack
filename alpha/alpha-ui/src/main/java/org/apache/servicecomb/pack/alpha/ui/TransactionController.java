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

package org.apache.servicecomb.pack.alpha.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.PagingGlobalTransactions;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetrics;
import org.apache.servicecomb.pack.alpha.ui.vo.DataTablesRequestDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.DataTablesResponseDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.EventDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.SubTransactionDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.TransactionRowDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.TransactionStatisticsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class TransactionController implements ApplicationListener<WebServerInitializedEvent> {

  @Autowired
  RestTemplate restTemplate;

  int serverPort;

  @PostMapping("/ui/transaction/sagalist")
  @ResponseBody
  public DataTablesResponseDTO sagaList(@ModelAttribute DataTablesRequestDTO dataTablesRequestDTO,
      HttpServletRequest request) {
    UriComponents uriComponents = UriComponentsBuilder
        .fromUriString("http://localhost:" + serverPort + "/alpha/api/v1/transaction")
        .queryParam("page", dataTablesRequestDTO.getStart()/dataTablesRequestDTO.getLength())
        .queryParam("size", dataTablesRequestDTO.getLength())
        .build();
    List<TransactionRowDTO> data = new ArrayList<>();
    ResponseEntity<PagingGlobalTransactions> entity = restTemplate
        .getForEntity(uriComponents.toUriString(), PagingGlobalTransactions.class);
    PagingGlobalTransactions pagingGlobalTransactions = entity.getBody();
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
      @ModelAttribute DataTablesRequestDTO dataTablesRequestDTO) {
    List<TransactionRowDTO> data = new ArrayList<>();
    GlobalTransaction globalTransaction = findGlobalTransactionByGlobalTxId(dataTablesRequestDTO.getQuery());
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
  }

  @GetMapping("/ui/transaction/{globalTxId}")
  public String getGlobalTransaction(ModelMap map, @PathVariable("globalTxId") String globalTxId) {
    List<EventDTO> events = new ArrayList<>();
    List<SubTransactionDTO> subTransactions = new ArrayList<>();
    GlobalTransaction globalTransaction = findGlobalTransactionByGlobalTxId(globalTxId);
    globalTransaction.getEvents().forEach(event -> {
      EventDTO eventDTO = EventDTO.builder()
          // Common Event properties
          .type(event.get("type").toString())
          .serviceName(event.get("serviceName").toString())
          .instanceId(event.get("instanceId").toString())
          .globalTxId(event.get("globalTxId").toString())
          .parentTxId(event.get("parentTxId").toString())
          .localTxId(event.get("localTxId").toString())
          .createTime(new Date(Long.valueOf(event.get("createTime").toString())))
          .build();
      if(eventDTO.getType().equals("TxStartedEvent")){
        // TxStartedEvent properties
        if(event.containsKey("compensationMethod")){
          eventDTO.setCompensationMethod(event.get("compensationMethod").toString());
        }
        if(event.containsKey("retries")){
          eventDTO.setRetries(Long.valueOf(event.get("retries").toString()));
        }
        if(event.containsKey("timeout")){
          eventDTO.setTimeout(Long.valueOf(event.get("timeout").toString()));
        }
      }
      if(eventDTO.getType().equals("TxAbortedEvent")){
        // TxAbortedEvent properties
        if(event.containsKey("payloads")){
          eventDTO.setException(event.get("payloads").toString());
        }
      }
      events.add(eventDTO);
    });

    globalTransaction.getSubTransactions().forEach( sub -> {
      subTransactions.add(
          SubTransactionDTO.builder().parentTxId(globalTxId).localTxId(sub.getLocalTxId())
              .beginTime(sub.getBeginTime()).endTime(sub.getEndTime())
              .durationTime(sub.getDurationTime()).state(sub.getState().name()).build());
    });
    map.put("events", events);
    map.put("globalTxId", globalTransaction.getGlobalTxId());
    map.put("subTransactions", subTransactions);
    return "transaction_details";
  }

  @GetMapping("/ui/transaction/statistics")
  @ResponseBody
  public TransactionStatisticsDTO getGlobalTransactionStatistics() {
    TransactionStatisticsDTO statisticsDTO = new TransactionStatisticsDTO();
    UriComponents uriComponents = UriComponentsBuilder
        .fromUriString("http://localhost:" + serverPort + "/alpha/api/v1/transaction/statistics")
        .build();
    ResponseEntity<Map> entity = restTemplate
        .getForEntity(uriComponents.toUriString(), Map.class);
    Map<String,Number> statistics = entity.getBody();
    if(statistics.containsKey("COMMITTED")){
      statisticsDTO.setSuccessful(statistics.get("COMMITTED").longValue());
    }
    if(statistics.containsKey("SUSPENDED")){
      statisticsDTO.setFailed(statistics.get("SUSPENDED").longValue());
    }
    if(statistics.containsKey("COMPENSATED")){
      statisticsDTO.setCompensated(statistics.get("COMPENSATED").longValue());
    }
    return statisticsDTO;
  }

  @GetMapping("/ui/transaction/slow")
  @ResponseBody
  public List<TransactionRowDTO> getSlowGlobalTransactionTopN() {
    List<TransactionRowDTO> transactionRowDTOS = new ArrayList<>();
    UriComponents uriComponents = UriComponentsBuilder
        .fromUriString("http://localhost:" + serverPort + "/alpha/api/v1/transaction/slow")
        .build();
    ResponseEntity<List<GlobalTransaction>> entity = restTemplate
        .exchange(uriComponents.toUriString(), HttpMethod.GET, null,
            new ParameterizedTypeReference<List<GlobalTransaction>>() {
            });
    List<GlobalTransaction> transactions = entity.getBody();
    transactions.stream().forEach( globalTransaction -> {
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

  private GlobalTransaction findGlobalTransactionByGlobalTxId(String globalTxId){
    UriComponents uriComponents = UriComponentsBuilder
        .fromUriString("http://localhost:" + serverPort + "/alpha/api/v1/transaction/"+globalTxId)
        .build();
    ResponseEntity<GlobalTransaction> entity = restTemplate
        .getForEntity(uriComponents.toUriString(), GlobalTransaction.class);
    GlobalTransaction globalTransaction = entity.getBody();
    return globalTransaction;
  }

  private AlphaMetrics getAlphaMetrics(){
    UriComponents uriComponents = UriComponentsBuilder
        .fromUriString("http://localhost:" + serverPort + "/alpha/api/v1/metrics")
        .build();
    ResponseEntity<AlphaMetrics> entity = restTemplate
        .getForEntity(uriComponents.toUriString(), AlphaMetrics.class);
    AlphaMetrics alphaMetrics = entity.getBody();
    return alphaMetrics;
  }

  @Override
  public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
    serverPort = webServerInitializedEvent.getWebServer().getPort();
  }
}
