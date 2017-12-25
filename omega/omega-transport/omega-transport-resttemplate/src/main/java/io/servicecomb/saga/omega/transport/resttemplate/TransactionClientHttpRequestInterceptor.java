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
 *
 */

package io.servicecomb.saga.omega.transport.resttemplate;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.servicecomb.saga.omega.context.IdGenerator;
import io.servicecomb.saga.omega.context.OmegaContext;

public class TransactionClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  public static final String GLOBAL_TX_ID_KEY = "X-Pack-Global-Transaction-Id";
  public static final String LOCAL_TX_ID_KEY = "X-Pack-Local-Transaction-Id";

  private final OmegaContext omegaContext;
  private final IdGenerator<String> idGenerator;

  TransactionClientHttpRequestInterceptor(OmegaContext omegaContext, IdGenerator<String> idGenerator) {
    this.omegaContext = omegaContext;
    this.idGenerator = idGenerator;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {

    request.getHeaders().add(GLOBAL_TX_ID_KEY, globalTxId());
    request.getHeaders().add(LOCAL_TX_ID_KEY, localTxId());
    return execution.execute(request, body);
  }

  private String globalTxId() {
    String globalTxId = omegaContext.globalTxId();

    if (globalTxId == null) {
      globalTxId = idGenerator.nextId();
      omegaContext.setGlobalTxId(globalTxId);
    }
    return globalTxId;
  }

  private String localTxId() {
    String localTxId = omegaContext.localTxId();

    if (localTxId == null) {
      localTxId = idGenerator.nextId();
      omegaContext.setLocalTxId(localTxId);
    }
    return localTxId;
  }
}
