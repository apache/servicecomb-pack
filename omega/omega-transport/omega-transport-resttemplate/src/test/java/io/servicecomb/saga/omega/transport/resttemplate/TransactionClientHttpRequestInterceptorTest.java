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

import static io.servicecomb.saga.omega.transport.resttemplate.TransactionClientHttpRequestInterceptor.TRANSACTION_ID_KEY;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@RunWith(JUnit4.class)
public class TransactionClientHttpRequestInterceptorTest {

  private HttpRequest request = mock(HttpRequest.class);

  private ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

  private ClientHttpResponse response = mock(ClientHttpResponse.class);

  private ClientHttpRequestInterceptor clientHttpRequestInterceptor = new TransactionClientHttpRequestInterceptor(
      new UniqueIdGenerator());

  @Test
  public void newTransactionIdInHeaderIfNonExists() throws IOException {
    when(request.getHeaders()).thenReturn(new HttpHeaders());

    when(execution.execute(request, null)).thenReturn(response);

    clientHttpRequestInterceptor.intercept(request, null, execution);

    assertThat(request.getHeaders().getOrDefault(TRANSACTION_ID_KEY, null), is(notNullValue()));
  }

  @Test
  public void sameTransactionIdInHeaderIfAlreadyExists() throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.add(TRANSACTION_ID_KEY, "txId");
    when(request.getHeaders()).thenReturn(headers);
    when(execution.execute(request, null)).thenReturn(response);

    clientHttpRequestInterceptor.intercept(request, null, execution);

    List<String> expected = Collections.singletonList("txId");
    assertThat(request.getHeaders().getOrDefault(TRANSACTION_ID_KEY, null), is(expected));
  }
}