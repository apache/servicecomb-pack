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

package org.apache.servicecomb.saga.transports.httpclient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SuccessfulSagaResponse;

import org.apache.servicecomb.saga.core.TransportFailedException;
import org.apache.servicecomb.saga.transports.RestTransport;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class HttpClientTransport implements RestTransport {

  private final int requestTimeout;
  private static final int DEFAULT_REQUEST_TIMEOUT = 30000;

  private static final Map<String, Function<URI, Request>> requestFactories = new HashMap<String, Function<URI, Request>>() {{
    put("GET", Request::Get);
    put("POST", Request::Post);
    put("PUT", Request::Put);
    put("DELETE", Request::Delete);
  }};

  public HttpClientTransport() {
    this(DEFAULT_REQUEST_TIMEOUT);
  }

  public HttpClientTransport(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  @Segment(name = "transport", category = "network", library = "kamon")
  @Override
  public SagaResponse with(String address, String path, String method, Map<String, Map<String, String>> params) {
    URIBuilder builder = new URIBuilder().setScheme("http").setHost(address).setPath(path);

    if (params.containsKey("query")) {
      for (Entry<String, String> entry : params.get("query").entrySet()) {
        builder.addParameter(entry.getKey(), entry.getValue());
      }
    }

    try {
      URI uri = builder.build();
      Request request = requestFactories.getOrDefault(
          method.toUpperCase(),
          exceptionThrowingFunction(method)).apply(uri);

      request.socketTimeout(requestTimeout);
      if (params.containsKey("json")) {
        request.bodyString(params.get("json").get("body"), ContentType.APPLICATION_JSON);
      }

      if (params.containsKey("form")) {
        Form form = Form.form();
        for (Entry<String, String> entry : params.get("form").entrySet()) {
          form.add(entry.getKey(), entry.getValue()).build();
        }
        request.bodyForm(form.build());
      }

      return this.on(request);
    } catch (URISyntaxException e) {
      throw new TransportFailedException("Wrong request URI", e);
    }
  }

  private Function<URI, Request> exceptionThrowingFunction(String method) {
    return u -> {
      throw new TransportFailedException("No such method " + method);
    };
  }

  private SagaResponse on(Request request) {
    try {
      HttpResponse httpResponse = request.execute().returnResponse();
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      String content = IOUtils.toString(new InputStreamReader(httpResponse.getEntity().getContent()));
      if (statusCode >= 200 && statusCode < 300) {
        return new SuccessfulSagaResponse(content);
      }
      throw new TransportFailedException("The remote service returned with status code " + statusCode
          + ", reason " + httpResponse.getStatusLine().getReasonPhrase()
          + ", and content " + content);
    } catch (IOException e) {
      throw new TransportFailedException("Network Error", e);
    }
  }
}
