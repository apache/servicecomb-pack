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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.servicecomb.saga.core.Compensation;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.Transaction;

class JsonSagaRequest implements SagaRequest {

  private final String id;
  private final String serviceName;
  private final JsonTransaction transaction;
  private final JsonCompensation compensation;
  private final String[] parents;

  @JsonCreator
  public JsonSagaRequest(
      @JsonProperty("id") String id,
      @JsonProperty("serviceName") String serviceName,
      @JsonProperty("transaction") JsonTransaction transaction,
      @JsonProperty("compensation") JsonCompensation compensation,
      @JsonProperty("parents") String[] parents) {

    this.id = id;
    this.serviceName = serviceName;
    this.transaction = transaction;
    this.compensation = compensation;
    this.parents = parents == null? new String[0] : parents;
  }

  @Override
  public void commit() {
  }

  @Override
  public void compensate() {
  }

  @Override
  public void abort(Exception e) {
  }

  @Override
  public Transaction transaction() {
    return transaction;
  }

  @Override
  public Compensation compensation() {
    return compensation;
  }

  @Override
  public String serviceName() {
    return serviceName;
  }

  @Override
  public String id() {
    return id;
  }

  String[] parents() {
    return parents;
  }
}
