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

package org.apache.servicecomb.saga.core.application.interpreter;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RestRequestChecker {

  private static final Set<String> validMethods = new HashSet<>(asList(
      "GET",
      "POST",
      "PUT",
      "DELETE"
  ));

  private RestRequestChecker() {
  }

  public static void checkParameters(String method, Map<String, Map<String, String>> params) {
    if (method == null || !validMethods.contains(method.toUpperCase())) {
      throw new IllegalArgumentException("Unsupported method " + method);
    }

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
