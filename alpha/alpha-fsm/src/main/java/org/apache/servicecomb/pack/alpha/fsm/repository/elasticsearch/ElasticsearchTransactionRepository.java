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

package org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepository;
import org.apache.servicecomb.pack.alpha.fsm.repository.model.GloablTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

public class ElasticsearchTransactionRepository implements TransactionRepository {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String INDEX_NAME = "alpha_global_transaction";
  private static final String INDEX_TYPE = "alpha_global_transaction_type";
  private final ElasticsearchTemplate template;
  private final MetricsService metricsService;
  private final ObjectMapper mapper = new ObjectMapper();
  private int batchSize;
  private int batchSizeCounter;
  private int refreshTime;
  private final List<IndexQuery> queries = new ArrayList<>();
  private final Boolean lock = true;

  public ElasticsearchTransactionRepository(
      ElasticsearchTemplate template, MetricsService metricsService, int batchSize,
      int refreshTime) {
    this.template = template;
    this.metricsService = metricsService;
    this.batchSize = batchSize;
    this.refreshTime = refreshTime;
    if (this.refreshTime > 0) {
      new Thread(new RefreshTimer(), "elasticsearch-repository-refresh").start();
    }
    if(!this.template.indexExists(INDEX_NAME)){
      this.template.createIndex(INDEX_NAME);
    }
  }

  @Override
  public void send(GloablTransaction transaction) throws Exception {
    long begin = System.currentTimeMillis();
    queries.add(convert(transaction));
    batchSizeCounter++;
    metricsService.metrics().doRepositoryReceived();
    if (batchSize == 0 || batchSizeCounter == batchSize) {
      synchronized (lock){
        save(begin);
        batchSizeCounter = 0;
        queries.clear();
      }
    }
  }

  private IndexQuery convert(GloablTransaction transaction) throws JsonProcessingException {
    IndexQuery indexQuery = new IndexQuery();
    indexQuery.setId(transaction.getGlobalTxId());
    indexQuery.setSource(mapper.writeValueAsString(transaction));
    indexQuery.setIndexName(INDEX_NAME);
    indexQuery.setType(INDEX_TYPE);
    return indexQuery;
  }

  private void save(long begin){
    template.bulkIndex(queries);
    template.refresh(INDEX_NAME);
    metricsService.metrics().doRepositoryAccepted(queries.size());
    long end = System.currentTimeMillis();
    metricsService.metrics().doRepositoryAvgTime((end - begin) / queries.size());
  }

  class RefreshTimer implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          synchronized (lock){
            if (!queries.isEmpty()) {
              save(System.currentTimeMillis());
            }
          }
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        } finally {
          try {
            Thread.sleep(refreshTime);
          } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
          }
        }
      }
    }
  }
}
