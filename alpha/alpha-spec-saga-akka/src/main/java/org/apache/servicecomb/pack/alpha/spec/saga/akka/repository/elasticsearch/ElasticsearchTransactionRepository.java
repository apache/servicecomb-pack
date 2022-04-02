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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.repository.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.PagingGlobalTransactions;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.properties.ElasticsearchProperties;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.repository.TransactionRepository;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;

public class ElasticsearchTransactionRepository implements TransactionRepository {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String INDEX_NAME = "alpha_global_transaction";
  public static final String INDEX_TYPE = "alpha_global_transaction_type";
  private static final long SCROLL_TIMEOUT = 3000;
  private final ElasticsearchRestTemplate template;
  private final MetricsService metricsService;
  private final ObjectMapper mapper = new ObjectMapper();
  private int batchSize;
  private int batchSizeCounter;
  private int refreshTime;
  private final List<IndexQuery> queries = new ArrayList<>();
  private final Object lock = new Object();

  public ElasticsearchTransactionRepository(ElasticsearchProperties elasticsearchProperties,
      ElasticsearchRestTemplate elasticsearchRestTemplate, MetricsService metricsService) {
    this.template = elasticsearchRestTemplate;
    this.batchSize = elasticsearchProperties.getBatchSize();
    this.refreshTime = elasticsearchProperties.getRefreshTime();
    this.metricsService = metricsService;
    if (this.refreshTime > 0) {
      new Thread(new RefreshTimer(), "elasticsearch-repository-refresh").start();
    }
    if (!this.template.indexExists(INDEX_NAME)) {
      this.template.createIndex(INDEX_NAME);
    }
  }

  @Override
  public void send(GlobalTransaction transaction) throws Exception {
    synchronized (lock) {
      long begin = System.currentTimeMillis();
      queries.add(convert(transaction));
      batchSizeCounter++;
      metricsService.metrics().doRepositoryReceived();
      if (batchSize == 0 || batchSizeCounter == batchSize) {
        save(begin);
        batchSizeCounter = 0;
      }
    }
  }

  @Override
  public GlobalTransaction getGlobalTransactionByGlobalTxId(String globalTxId) {
    Query query = new NativeSearchQueryBuilder().withIds(Collections.singletonList(globalTxId)).build();
    SearchHit<GlobalTransactionDocument> result =  this.template.searchOne(query, GlobalTransactionDocument.class);
    return result.getContent();
  }

  @Override
  public PagingGlobalTransactions getGlobalTransactions(int page, int size) {
    return getGlobalTransactions(null, page, size);
  }

  @Override
  public PagingGlobalTransactions getGlobalTransactions(String state, int page, int size) {
    long start = System.currentTimeMillis();
    PagingGlobalTransactions pagingGlobalTransactions;
    List<GlobalTransaction> globalTransactions = new ArrayList();
    try{
      if (this.template.indexOps(IndexCoordinates.of(INDEX_NAME)).exists()) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //queryBuilder.withSearchType(SearchType.valueOf(INDEX_TYPE));
        if (state != null && state.trim().length() > 0) {
          queryBuilder.withQuery(QueryBuilders.termQuery("state.keyword", state));
        } else {
          queryBuilder.withQuery(QueryBuilders.matchAllQuery());
        }
        queryBuilder.withSort(SortBuilders.fieldSort("beginTime").order(SortOrder.DESC).unmappedType("date"));
        queryBuilder.withPageable(PageRequest.of(page, size));

        SearchHits<GlobalTransactionDocument> result = this.template.search(queryBuilder.build(), GlobalTransactionDocument.class);
        result.forEach(hit -> {
          try {
            globalTransactions.add(hit.getContent());
          } catch (Exception e) {
            LOG.error(e.getMessage(), e);
          }
        });
        pagingGlobalTransactions = PagingGlobalTransactions.builder().page(page).size(size).total(result.getTotalHits())
            .globalTransactions(globalTransactions).elapsed(System.currentTimeMillis() - start).build();
      } else {
        LOG.warn("[alpha_global_transaction] index not exist");
        pagingGlobalTransactions = PagingGlobalTransactions.builder().page(page).size(size).total(0)
            .globalTransactions(globalTransactions).elapsed(System.currentTimeMillis() - start).build();
      }
    }catch (Exception ex){
      LOG.error(ex.getMessage(),ex);
      pagingGlobalTransactions = PagingGlobalTransactions.builder().page(page).size(size).total(0)
          .globalTransactions(globalTransactions).elapsed(System.currentTimeMillis() - start).build();
    }
    LOG.info("Query total hits {}, return page {}, size {}", pagingGlobalTransactions.getTotal(), page, size);
    return pagingGlobalTransactions;
  }

  public Map<String, Long> getTransactionStatistics() {
    Map<String, Long> statistics = new HashMap<>();

    Query query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("count_group_by_state").field("state.keyword"))
        .build();
    SearchHits<Map> result = this.template.search(query,Map.class,IndexCoordinates.of(INDEX_NAME));
    if (result.getTotalHits() > 0) {
      final ParsedStringTerms groupState = result.getAggregations().get("count_group_by_state");
      statistics = groupState.getBuckets()
          .stream()
          .collect(Collectors.toMap(MultiBucketsAggregation.Bucket::getKeyAsString,
              MultiBucketsAggregation.Bucket::getDocCount));
    }

    return statistics;
  }

  @Override
  public List<GlobalTransaction> getSlowGlobalTransactionsTopN(int n) {
    List<GlobalTransaction> globalTransactions = new ArrayList();
    Query query = new NativeSearchQueryBuilder()
        .withSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .withQuery(QueryBuilders.matchAllQuery())
        .withSort(SortBuilders.fieldSort("durationTime").order(SortOrder.DESC))
        .withPageable(PageRequest.of(0,n))
        .build();
    SearchHits<GlobalTransactionDocument> result = this.template.search(query,GlobalTransactionDocument.class);
    result.forEach(hit -> {
      globalTransactions.add(hit.getContent());
    });
    return globalTransactions;
  }

  private IndexQuery convert(GlobalTransaction transaction) throws JsonProcessingException {
    IndexQuery indexQuery = new IndexQuery();
    indexQuery.setId(transaction.getGlobalTxId());
    indexQuery.setSource(mapper.writeValueAsString(transaction));
    return indexQuery;
  }

  private void save(long begin) {
    template.bulkIndex(queries, IndexCoordinates.of(INDEX_NAME));
    template.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
    metricsService.metrics().doRepositoryAccepted(queries.size());
    long end = System.currentTimeMillis();
    metricsService.metrics().doRepositoryAvgTime((end - begin) / queries.size());
    if (LOG.isDebugEnabled()) {
      LOG.debug("save queries={}, received={}, accepted={}", queries.size(),
          metricsService.metrics().getRepositoryReceived(),
          metricsService.metrics().getRepositoryAccepted());
    }
    queries.clear();
  }

  class RefreshTimer implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          synchronized (lock) {
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
            Thread.currentThread().interrupt();
            LOG.error(e.getMessage(), e);
          }
        }
      }
    }
  }
}
