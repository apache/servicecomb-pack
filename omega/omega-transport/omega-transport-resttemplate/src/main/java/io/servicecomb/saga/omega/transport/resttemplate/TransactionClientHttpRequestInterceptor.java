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

import io.servicecomb.saga.core.IdGenerator;

public class TransactionClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  public static final String TRANSACTION_ID_KEY = "X-Transaction-Id";

  private final IdGenerator<String> idGenerator;

  public TransactionClientHttpRequestInterceptor(IdGenerator<String> idGenerator) {
    this.idGenerator = idGenerator;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    if (!request.getHeaders().containsKey(TRANSACTION_ID_KEY)) {
      String txId = idGenerator.nextId();
      request.getHeaders().add(TRANSACTION_ID_KEY, txId);
    }
    return execution.execute(request, body);
  }
}
