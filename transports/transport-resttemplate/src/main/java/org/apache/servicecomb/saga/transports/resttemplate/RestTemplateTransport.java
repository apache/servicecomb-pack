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

package org.apache.servicecomb.saga.transports.resttemplate;

import static java.util.Collections.emptyMap;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SuccessfulSagaResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.apache.servicecomb.saga.core.TransportFailedException;
import org.apache.servicecomb.saga.transports.RestTransport;

public class RestTemplateTransport implements RestTransport {

  private final String protocol;
  private final RestTemplate restTemplate;
  private final Map<String, BiFunction<String, Map<String, Map<String, String>>, ResponseEntity<String>>> methodMapping = new HashMap<>();

  public RestTemplateTransport(RestTemplate restTemplate, String protocol) {
    this.protocol = protocol;
    this.restTemplate = restTemplate;
    this.restTemplate.setErrorHandler(new ResponseErrorHandler() {
      @Override
      public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
        return false;
      }

      @Override
      public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
      }
    });

    methodMapping.put(GET.name(), exchange(GET));
    methodMapping.put(PUT.name(), exchange(PUT));
    methodMapping.put(POST.name(), exchange(POST));
    methodMapping.put(DELETE.name(), exchange(DELETE));
  }

  @Override
  public SagaResponse with(String address, String path, String method, Map<String, Map<String, String>> params) {
    String url = buildUrl(address, path, params);

    try {
      ResponseEntity<String> responseEntity = methodHandler(method).apply(url, params);
      return new SuccessfulSagaResponse(responseEntity.getBody());
    } catch (Throwable e) {
      throw new TransportFailedException(
          String.format("The remote service %s failed to serve the %s request to %s ", address, method, path),
          e);
    }
  }

  private String buildUrl(String address, String path, Map<String, Map<String, String>> params) {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath(path);
    params.getOrDefault(SagaRequest.PARAM_QUERY, emptyMap())
        .forEach(uriComponentsBuilder::queryParam);

    return protocol + address + uriComponentsBuilder.build().toString();
  }

  private BiFunction<String, Map<String, Map<String, String>>, ResponseEntity<String>> methodHandler(String method) {
    return methodMapping.getOrDefault(method.toUpperCase(), (url, params) -> {
      throw new TransportFailedException("No such method " + method);
    });
  }

  private BiFunction<String, Map<String, Map<String, String>>, ResponseEntity<String>> exchange(HttpMethod method) {
    return (url, params) -> restTemplate.exchange(url, method, request(params), String.class);
  }

  private HttpEntity<Object> request(Map<String, Map<String, String>> params) {
    HttpHeaders headers = new HttpHeaders();

    if (params.containsKey(SagaRequest.PARAM_JSON)) {
      headers.setContentType(APPLICATION_JSON);
      return new HttpEntity<>(params.get(SagaRequest.PARAM_JSON).get(SagaRequest.PARAM_JSON_BODY), headers);
    }

    if (params.containsKey(SagaRequest.PARAM_FORM)) {
      headers.setContentType(APPLICATION_FORM_URLENCODED);
      return new HttpEntity<>(params.get(SagaRequest.PARAM_FORM), headers);
    }

    return null;
  }
}
