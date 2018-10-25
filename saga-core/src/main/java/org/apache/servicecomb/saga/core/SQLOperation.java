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

import java.util.Collections;
import java.util.List;

public class SQLOperation implements Operation {

  private final String sql;
  private final int retries;
  private final List<List<String>> params;

  public SQLOperation(String sql, int retries, List<List<String>> params) {
    this.sql = sql;
    this.retries = retries;
    this.params = params == null ? Collections.<List<String>>emptyList() : params;
  }

  public String sql() {
    return sql;
  }

  public List<List<String>> params() {
    return params;
  }

  @Override
  public String toString() {
    return "SQLOperation{" +
        "sql='" + sql + '\'' +
        ", params=" + params +
        '}';
  }

  @Override
  public SagaResponse send(String datasource) {
    return SUCCESSFUL_SAGA_RESPONSE;
  }

  @Override
  public SagaResponse send(String datasource, SagaResponse response) {
    return send(datasource);
  }

  @Override
  public int retries() {
    return this.retries;
  }
}
