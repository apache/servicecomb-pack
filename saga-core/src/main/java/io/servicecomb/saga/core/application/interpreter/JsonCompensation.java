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

package io.servicecomb.saga.core.application.interpreter;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.servicecomb.saga.core.Compensation;
import java.util.Map;

class JsonCompensation implements Compensation {

  private final String path;
  private final String method;
  private final Map<String, Map<String, String>> params;

  @JsonCreator
  public JsonCompensation(
      @JsonProperty("path") String path,
      @JsonProperty("method") String method,
      @JsonProperty("params") Map<String, Map<String, String>> params) {

    RestRequestChecker.checkParameters(method, params);

    this.path = path;
    this.method = method;
    this.params = params == null? emptyMap() : params;
  }

  @Override
  public void run() {

  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String method() {
    return method;
  }

  @Override
  public Map<String, Map<String, String>> params() {
    return params;
  }
}
