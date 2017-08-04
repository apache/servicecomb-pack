/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.transports.httpclient;

import io.servicecomb.saga.core.FailedSagaResponse;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SuccessfulSagaResponse;
import io.servicecomb.saga.core.Transport;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.core.util.IOUtils;

public class HttpClientTransport implements Transport {

  // TODO: 8/3/2017 transport.with(name, path, method).on()
  @Override
  public SagaResponse with(String serviceName, String path, String method) {
    Request request;
    if ("GET".equals(method)) {
      request = Request.Get("http://" + serviceName + path);
    } else {
      request = Request.Post("http://" + serviceName + path);
    }
    return this.on(request);
  }

  private SagaResponse on(Request request) {
    try {
      HttpResponse httpResponse = request.execute().returnResponse();
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      String content = IOUtils.toString(new InputStreamReader(httpResponse.getEntity().getContent()));
      if (statusCode >= 200 && statusCode < 300) {
        return new SuccessfulSagaResponse(statusCode, content);
      }
      return new FailedSagaResponse(statusCode, httpResponse.getStatusLine().getReasonPhrase(), content);
    } catch (IOException e) {
      return new FailedSagaResponse("Network Error", e);
    }
  }
}
