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

import java.util.Map;

class RestRequestChecker {

  private RestRequestChecker() {
  }

  static void checkParameters(String method, Map<String, Map<String, String>> params) {
    if (isDeleteOrGet(method) && hasBody(params)) {
      throw new IllegalArgumentException("GET & DELETE request cannot enclose a body");
    }
  }

  private static boolean isDeleteOrGet(String method) {
    return "GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
  }

  private static boolean hasBody(Map<String, Map<String, String>> params) {
    return params != null && (params.containsKey("form") || params.containsKey("json"));
  }
}