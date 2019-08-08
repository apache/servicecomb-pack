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

package org.apache.servicecomb.pack.alpha.server.api;

import java.util.List;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetrics;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.PagingGlobalTransactions;
import org.apache.servicecomb.pack.alpha.server.metrics.AlphaMetricsEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alpha/api/v1")
public class APIControllerV1 {

  @Autowired
  AlphaMetricsEndpoint alphaMetricsEndpoint;

  @Autowired(required = false)
  TransactionRepository transactionRepository;

  @GetMapping(value = "/metrics")
  ResponseEntity<AlphaMetrics> metrics() {
    AlphaMetrics alphaMetrics = new AlphaMetrics();
    alphaMetrics.setMetrics(alphaMetricsEndpoint.getMetrics());
    alphaMetrics.setNodeType(alphaMetricsEndpoint.getNodeType());
    return ResponseEntity.ok(alphaMetrics);
  }

  @GetMapping(value = "/transaction/{globalTxId}")
  ResponseEntity<GlobalTransaction> getTransactionByGlobalTxId(@PathVariable String globalTxId)
      throws Exception {
    GlobalTransaction globalTransaction = transactionRepository
        .getGlobalTransactionByGlobalTxId(globalTxId);
    return ResponseEntity.ok(globalTransaction);
  }

  @GetMapping(value = "/transaction")
  ResponseEntity<PagingGlobalTransactions> getTransactions(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "size", required = false, defaultValue = "50") int size)
      throws Exception {
    PagingGlobalTransactions pagingGlobalTransactions = transactionRepository
        .getGlobalTransactions(page, size);
    return ResponseEntity.ok(pagingGlobalTransactions);
  }

  @GetMapping(value = "/transaction/statistics")
  ResponseEntity<Map<String,Long>> getTransactions() {
    return ResponseEntity.ok(transactionRepository.getTransactionStatistics());
  }

  @GetMapping(value = "/transaction/slow")
  ResponseEntity<List<GlobalTransaction>> getSlowTransactions() {
    return ResponseEntity.ok(transactionRepository.getSlowGlobalTransactionsTopN(10));
  }
}
