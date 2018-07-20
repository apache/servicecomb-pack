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

import static org.apache.servicecomb.saga.core.Fallback.NOP_FALLBACK;

public interface SagaRequest {

  String PARAM_FORM = "form";
  String PARAM_JSON = "json";
  String PARAM_JSON_BODY = "body";
  String PARAM_QUERY = "query";

  Transaction transaction();

  Compensation compensation();

  default Fallback fallback() {
    return NOP_FALLBACK;
  }

  String serviceName();

  String id();

  String type();

  String task();

  String[] parents();

  int failRetryDelayMilliseconds();
}
