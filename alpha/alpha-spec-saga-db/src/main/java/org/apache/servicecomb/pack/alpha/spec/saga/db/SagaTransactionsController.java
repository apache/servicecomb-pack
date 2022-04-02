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

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.db.model.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.invoke.MethodHandles;
import java.util.*;

@Controller
@RequestMapping("/saga")

public class SagaTransactionsController {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventEnvelopeRepository eventRepository;

  SagaTransactionsController(TxEventEnvelopeRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @GetMapping(value = "/stats")
  public ResponseEntity<Stats> getStats() {
        /*
        This function gives returns the count of the transactions listed below
        1. Total Transactions
        2. PENDING
        3. COMMITTED
        4. COMPENSATING
        5. ROLLBACKED
        6. FAILURERATE

        Sample Response
        {
            "totalTransaction": 10,
            "PENDING": 3,
            "COMMITTED": 1,
            "COMPENSATING": 2,
            "ROLLBACKED": 4,
            "FAILURERATE": 60,
            "updatedAt": 1540450937
        }
         */
    Stats oStats = new Stats(eventRepository.findTotalCountOfTransactions(), eventRepository.findCountOfPendingEvents(),
        eventRepository.findCountOfCommittedEvents(), eventRepository.findCountOfCompensatingEvents(),
        eventRepository.findCountOfRollBackedEvents());
    return ResponseEntity.ok(oStats);
  }


  @GetMapping(value = "/recent")
  public ResponseEntity<Collection<SagaTransactionsController.TxEventVo>> recentTransactions(
      @RequestParam(name = "status") String status, @RequestParam(name = "count") int count) {
        /*
        This will return the list of recent transactions
        Parameters :
        status - Status of transaction (PENDING, COMMITTED, COMPENSATING, ROLLBACKED)
        count - Number of last transactions to be returned (max: 15, default: 5)

        Sample Response
        [
            {
                "surrogateId": 6,
                "serviceName": "booking",
                "instanceId": "booking-172.23.0.7",
                "creationTime": 1540823579335,
                "globalTxId": "daa74121-9e00-4567-96b7-dc9fd163060b",
                "localTxId": "daa74121-9e00-4567-96b7-dc9fd163060b",
                "parentTxId": null,
                "type": "SagaEndedEvent",
                "compensationMethod": "",
                "expiryTime": 253402214400000,
                "retryMethod": "",
                "retries": 0,
                "payloads": "AQE="
            }
        ]
         */
    Iterable<TxEvent> events;

    List<SagaTransactionsController.TxEventVo> eventVos = new LinkedList<>();
    switch (status) {
      case "PENDING":
        events = eventRepository.findPendingEvents(PageRequest.of(0, count));
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "COMMITTED":
        events = eventRepository.findCommittedEvents(PageRequest.of(0, count));
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "COMPENSATING":
        events = eventRepository.findCompensatingEvents(PageRequest.of(0, count));
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "ROLLBACKED":
        events = eventRepository.findRollBackedEvents(PageRequest.of(0, count));
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      default:
        LOG.info("Unknown Status");
    }
    return ResponseEntity.ok(eventVos);
  }

  @GetMapping(value = "/transactions")
  public ResponseEntity<Collection<SagaTransactionsController.TxEventVo>> getTransactions(
      @RequestParam(name = "status") String status) {
        /*
        This will return the list of transactions with pagination enabled.
        Parameters :
            status - Status of transaction (ALL, PENDING, COMMITTED, COMPENSATING, ROLLBACKED)
            pagination - is pagination enabled
            countPerPage - List of elements per page
            pageNumber - Current Page to be sent

        Sample Response
        [
            {
                "surrogateId": 6,
                "serviceName": "booking",
                "instanceId": "booking-172.23.0.7",
                "creationTime": 1540823579335,
                "globalTxId": "daa74121-9e00-4567-96b7-dc9fd163060b",
                "localTxId": "daa74121-9e00-4567-96b7-dc9fd163060b",
                "parentTxId": null,
                "type": "SagaEndedEvent",
                "compensationMethod": "",
                "expiryTime": 253402214400000,
                "retryMethod": "",
                "retries": 0,
                "payloads": "AQE="
            }
        ]
         */
    //TODO Pagination

    Iterable<TxEvent> events;

    List<SagaTransactionsController.TxEventVo> eventVos = new LinkedList<>();

    switch (status) {
      case "PENDING":
        events = eventRepository.findPendingEvents();
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "COMMITTED":
        events = eventRepository.findCommittedEvents();
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "COMPENSATING":
        events = eventRepository.findCompensatingEvents();
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "ROLLBACKED":
        events = eventRepository.findRollBackedEvents();
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      case "ALL":
        events = eventRepository.findAll();
        events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
        break;
      default:
        LOG.info("Unknown Status");
    }

    return ResponseEntity.ok(eventVos);
  }
  
  @GetMapping(value = "/findTransactions")
  public ResponseEntity<Collection<SagaTransactionsController.TxEventVo>> findTransactions(
      @RequestParam(required = false, name = "globalTxID") Object globalTxID,
      @RequestParam(required = false, name = "microServiceName") String microServiceName) {
        /*
        This will return all the list of sub-transactions  for a particular Global ID or MicroService.
        Parameter :
            globalID : GlobalID of the transactions
            microserviceName : Name of the Microservice
        Sample Response
        [
            {
                "surrogateId": 6,
                "serviceName": "booking",
                "instanceId": "booking-172.23.0.7",
                "creationTime": 1540823579335,
                "globalTxId": "daa74121-9e00-4567-96b7-dc9fd163060b",
                "localTxId": "daa74121-9e00-4567-96b7-dc9fd163060b",
                "parentTxId": null,
                "type": "SagaEndedEvent",
                "compensationMethod": "",
                "expiryTime": 253402214400000,
                "retryMethod": "",
                "retries": 0,
                "payloads": "AQE="
            }
        ]
         */

    Iterable<TxEvent> events;
    if (globalTxID != null) {
      events = eventRepository.findByGlobalTxId(globalTxID.toString());
    } else if (!"".equals(microServiceName)) {
      events = eventRepository.findByServiceName(microServiceName);
    } else {
      events = null;
    }

    Collection<TxEventVo> eventVos = new LinkedList<>();
    if (events != null) {
      events.forEach(event -> eventVos.add(new SagaTransactionsController.TxEventVo(event)));
    }

    return ResponseEntity.ok(eventVos);
  }

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  private static class TxEventVo extends TxEvent {
    private TxEventVo(TxEvent event) {
      super(event);
    }
  }
}
