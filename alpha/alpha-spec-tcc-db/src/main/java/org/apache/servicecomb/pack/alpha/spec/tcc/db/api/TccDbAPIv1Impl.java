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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.api;

import java.util.List;
import java.util.Map;
import javax.transaction.NotSupportedException;
import org.apache.servicecomb.pack.alpha.core.api.APIv1;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.GlobalTransaction;
import org.apache.servicecomb.pack.alpha.core.fsm.repository.model.PagingGlobalTransactions;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetrics;
import org.apache.servicecomb.pack.alpha.core.metrics.AlphaMetricsEndpoint;
import org.springframework.beans.factory.annotation.Autowired;

public class TccDbAPIv1Impl implements APIv1 {

  @Autowired
  AlphaMetricsEndpoint alphaMetricsEndpoint;

  public AlphaMetrics getMetrics() {
    AlphaMetrics alphaMetrics = new AlphaMetrics();
    alphaMetrics.setMetrics(alphaMetricsEndpoint.getMetrics());
    alphaMetrics.setNodeType(alphaMetricsEndpoint.getNodeType());
    return alphaMetrics;
  }

  @Override
  public GlobalTransaction getTransactionByGlobalTxId(String globalTxId) throws Exception {
    throw new NotSupportedException();
  }

  @Override
  public PagingGlobalTransactions getTransactions(String state, int page, int size)
      throws Exception {
    throw new NotSupportedException();
  }

  @Override
  public Map<String, Long> getTransactionStatistics() throws Exception {
    throw new NotSupportedException();
  }

  @Override
  public List<GlobalTransaction> getSlowTransactions(int size) throws Exception {
    throw new NotSupportedException();
  }
}
