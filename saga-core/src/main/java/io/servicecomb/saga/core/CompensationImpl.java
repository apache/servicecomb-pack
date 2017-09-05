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

package io.servicecomb.saga.core;

import static java.util.Collections.emptyMap;

import io.servicecomb.saga.core.application.interpreter.RestRequestChecker;
import java.util.Map;

public class CompensationImpl implements Compensation {

  protected final String path;
  protected final String method;
  protected final Map<String, Map<String, String>> params;

  public CompensationImpl(String path, String method, Map<String, Map<String, String>> params) {
    RestRequestChecker.checkParameters(method, params);

    this.path = path;
    this.method = method;
    this.params = params == null? emptyMap() : params;
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

  @Override
  public String toString() {
    return "Compensation{" +
        "path='" + path + '\'' +
        ", method='" + method + '\'' +
        ", params=" + params +
        '}';
  }
}
