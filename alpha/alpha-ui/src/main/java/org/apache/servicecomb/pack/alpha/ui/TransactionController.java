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
import java.util.UUID;
import javax.websocket.server.PathParam;
import org.apache.servicecomb.pack.alpha.ui.vo.DataTablesRequestDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.DataTablesResponseDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.EventDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.SubTransactionDTO;
import org.apache.servicecomb.pack.alpha.ui.vo.TransactionRowDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TransactionController {

  @PostMapping("/ui/transaction/sagalist")
  @ResponseBody
  public DataTablesResponseDTO sagaList(@ModelAttribute DataTablesRequestDTO dataTablesRequestDTO) {
    List<TransactionRowDTO> data = new ArrayList<>();
    for (int i = 0; i < dataTablesRequestDTO.getLength()-2; i++) {
      data.add(TransactionRowDTO.builder().serviceName("Booking").instanceId("booking-1")
          .globalTxId("xxxx-xxx-xxx").state("COMMITTED").beginTime(new Date()).endTime(new Date())
          .subTxSize(3).durationTime(109).build());
    }
    for (int i = 0; i < 1; i++) {
      data.add(TransactionRowDTO.builder().serviceName("Booking").instanceId("booking-1")
          .globalTxId("xxxx-xxx-xxx").state("SUSPENDED").beginTime(new Date()).endTime(new Date())
          .subTxSize(3).durationTime(109).build());
    }
    for (int i = 0; i < 1; i++) {
      data.add(TransactionRowDTO.builder().serviceName("Booking").instanceId("booking-1")
          .globalTxId("xxxx-xxx-xxx").state("COMPENSATED").beginTime(new Date()).endTime(new Date())
          .subTxSize(3).durationTime(109).build());
    }
    return DataTablesResponseDTO.builder()
        .draw(dataTablesRequestDTO.getDraw())
        .recordsTotal(100)
        .recordsFiltered(100)
        .data(data)
        .build();
  }

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
  public DataTablesResponseDTO searchList(@ModelAttribute DataTablesRequestDTO dataTablesRequestDTO) {
    List<TransactionRowDTO> data = new ArrayList<>();
    data.add(TransactionRowDTO.builder().serviceName("Booking").instanceId("booking-1")
        .globalTxId("xxxx-xxx-xxx").state("SUSPENDED").beginTime(new Date()).endTime(new Date())
        .subTxSize(3).durationTime(109).build());
    return DataTablesResponseDTO.builder()
        .draw(dataTablesRequestDTO.getDraw())
        .recordsTotal(1)
        .recordsFiltered(1)
        .data(data)
        .build();
  }

  @GetMapping("/ui/transaction/{globalTxId}")
  public String searchList(ModelMap map, @PathParam("globalTxId") String globalTxId) {
    List<EventDTO> events = new ArrayList<>();
    List<SubTransactionDTO> subTransactions = new ArrayList<>();

    globalTxId = UUID.randomUUID().toString();
    String localTxId_1 = UUID.randomUUID().toString();
    String localTxId_2 = UUID.randomUUID().toString();
    String localTxId_3 = UUID.randomUUID().toString();
    events.add(EventDTO.builder().type("SagaStartedEvent").serviceName("Booking").globalTxId(globalTxId).instanceId("booking-1").createTime(new Date()).localTxId(globalTxId).parentTxId(globalTxId).timeout(60000).build());
    events.add(EventDTO.builder().type("TxStartedEvent").serviceName("Car").globalTxId(globalTxId).instanceId("car-1").createTime(new Date()).localTxId(localTxId_1).parentTxId(globalTxId).retries(3).compensationMethod("org.servicecomb.sample.car.CarService.cannelOrder()").build());
    events.add(EventDTO.builder().type("TxEndedEvent").serviceName("Car").globalTxId(globalTxId).instanceId("car-1").createTime(new Date()).localTxId(localTxId_1).parentTxId(globalTxId).build());
    events.add(EventDTO.builder().type("TxStartedEvent").serviceName("Hotel").globalTxId(globalTxId).instanceId("hotel-1").createTime(new Date()).localTxId(localTxId_2).parentTxId(globalTxId).retries(3).compensationMethod("org.servicecomb.sample.hotel.HotelService.cannelOrder()").build());
    events.add(EventDTO.builder().type("TxEndedEvent").serviceName("Hotel").globalTxId(globalTxId).instanceId("hotel-1").createTime(new Date()).localTxId(localTxId_2).parentTxId(globalTxId).build());
    events.add(EventDTO.builder().type("TxStartedEvent").serviceName("Flight").globalTxId(globalTxId).instanceId("flight-1").createTime(new Date()).localTxId(localTxId_3).parentTxId(globalTxId).retries(2).compensationMethod("org.servicecomb.sample.flight.FlightService.cannelOrder()").build());
    events.add(EventDTO.builder().type("TxEndedEvent").serviceName("Flight").globalTxId(globalTxId).instanceId("flight-1").createTime(new Date()).localTxId(localTxId_3).parentTxId(globalTxId).build());
    events.add(EventDTO.builder().type("TxAbortedEvent").serviceName("Flight").globalTxId(globalTxId).instanceId("flight-1").createTime(new Date()).localTxId(localTxId_3).parentTxId(globalTxId).exception("java.lang.NullPointerException\n"
        + "at TestCompile.work(TestCompile.java:25)\n"
        + "at TestCompile.main(TestCompile.java:17)").build());
    events.add(EventDTO.builder().type("SagaEndedEvent").serviceName("Booking").globalTxId(globalTxId).instanceId("booking-1").createTime(new Date()).localTxId(globalTxId).parentTxId(globalTxId).build());

    subTransactions.add(SubTransactionDTO.builder().parentTxId(globalTxId).localTxId(localTxId_1).beginTime(new Date()).endTime(new Date()).durationTime(10).state("COMMITTED").build());
    subTransactions.add(SubTransactionDTO.builder().parentTxId(globalTxId).localTxId(localTxId_2).beginTime(new Date()).endTime(new Date()).durationTime(10).state("COMMITTED").build());
    subTransactions.add(SubTransactionDTO.builder().parentTxId(globalTxId).localTxId(localTxId_3).beginTime(new Date()).endTime(new Date()).durationTime(10).state("COMMITTED").build());
    map.put("events",events);
    map.put("globalTxId",globalTxId);
    map.put("subTransactions",subTransactions);
    return "transaction_details";
  }

}
