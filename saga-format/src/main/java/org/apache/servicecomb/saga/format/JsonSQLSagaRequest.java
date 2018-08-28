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

import static org.apache.servicecomb.saga.format.JacksonFallback.NOP_TRANSPORT_AWARE_FALLBACK;

import org.apache.servicecomb.saga.core.Fallback;
import org.apache.servicecomb.saga.core.Operation;
import org.apache.servicecomb.saga.core.SagaRequestImpl;
import org.apache.servicecomb.saga.transports.SQLTransport;
import org.apache.servicecomb.saga.transports.TransportFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonSQLSagaRequest extends SagaRequestImpl implements JsonSagaRequest<SQLTransport> {

  private JacksonSQLTransaction transaction;
  private JacksonSQLCompensation compensation;

  @JsonCreator
  public JsonSQLSagaRequest(
      @JsonProperty("id") String id,
      @JsonProperty("datasource") String datasource,
      @JsonProperty("type") String type,
      @JsonProperty("transaction") JacksonSQLTransaction transaction,
      @JsonProperty("compensation") JacksonSQLCompensation compensation,
      @JsonProperty("fallback") Fallback fallback,
      @JsonProperty("parents") String[] parents,
      @JsonProperty("failRetryDelayMilliseconds") int failRetryDelayMilliseconds) {
    super(id, datasource, type, transaction, compensation,
        fallback == null ? NOP_TRANSPORT_AWARE_FALLBACK : fallback,
        parents, failRetryDelayMilliseconds);

    checkNull(transaction, "transaction");
    checkNull(compensation, "compensation");

    this.transaction = transaction;
    this.compensation = compensation;
  }

  @Override
  public JsonSagaRequest with(TransportFactory transportFactory) {
    transaction.with(transportFactory);
    compensation.with(transportFactory);
    return this;
  }

  private void checkNull(Operation operation, String operationName) {
    if (operation == null) {
      throw new IllegalArgumentException("Invalid request with NO " + operationName + " specified");
    }
  }
}
