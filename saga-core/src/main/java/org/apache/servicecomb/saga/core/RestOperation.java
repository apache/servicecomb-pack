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

package org.apache.servicecomb.saga.core;

import org.apache.servicecomb.saga.core.application.interpreter.RestRequestChecker;
import java.util.Map;

public class RestOperation implements Operation {

  private final String path;
  private final String method;
  private final Map<String, Map<String, String>> params;

  public RestOperation(String path, String method, Map<String, Map<String, String>> params) {
    RestRequestChecker.checkParameters(method, params);

    this.path = path;
    this.method = method;
    this.params = params == null? java.util.Collections.<String, Map<String, String>>emptyMap() : params;
  }

  public String path() {
    return path;
  }

  public String method() {
    return method;
  }

  public Map<String, Map<String, String>> params() {
    return params;
  }

  @Override
  public String toString() {
    return "Operation{" +
        "path='" + path + '\'' +
        ", method='" + method + '\'' +
        ", params=" + params +
        '}';
  }

  @Override
  public SagaResponse send(String address) {
    return SUCCESSFUL_SAGA_RESPONSE;
  }

  @Override
  public SagaResponse send(String address, SagaResponse response) {
    return send(address);
  }
}
