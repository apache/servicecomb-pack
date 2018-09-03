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

package org.apache.servicecomb.saga.format;

import java.util.List;

import org.apache.servicecomb.saga.core.Compensation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JacksonSQLCompensation extends JacksonSQLOperation implements Compensation {

  private final int retries;

  public JacksonSQLCompensation(String sql, List<List<Object>> params) {
    this(sql, params, DEFAULT_RETRIES);
  }

  @JsonCreator
  public JacksonSQLCompensation(
      @JsonProperty("sql") String sql,
      @JsonProperty("params") List<List<Object>> params,
      @JsonProperty("retries") int retries) {
    super(sql, params);
    this.retries = retries <= 0? DEFAULT_RETRIES : retries;
  }

  @Override
  public int retries() {
    return this.retries;
  }
}
