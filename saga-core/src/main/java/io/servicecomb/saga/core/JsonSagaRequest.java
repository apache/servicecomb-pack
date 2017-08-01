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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonSagaRequest implements SagaRequest {

  private final String id;
  private final String serviceName;
  private final Transaction transaction;
  private final Compensation compensation;
  private final String[] parents;

  @JsonCreator
  public JsonSagaRequest(
      @JsonProperty("id") String id,
      @JsonProperty("serviceName") String serviceName,
      @JsonProperty("transaction") Transaction transaction,
      @JsonProperty("compensation") Compensation compensation,
      @JsonProperty("parents") String[] parents) {

    this.id = id;
    this.serviceName = serviceName;
    this.transaction = transaction;
    this.compensation = compensation;
    this.parents = parents == null? new String[0] : parents;
  }

  public JsonSagaRequest(Transaction transaction, Compensation compensation) {
    this("saga", "Saga", transaction, compensation, new String[0]);
  }

  @Override
  public void commit() {
    transaction.run();
  }

  @Override
  public void compensate() {
    compensation.run();
  }

  @Override
  public void abort(Exception e) {
    compensation.run();
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

  public String[] parents() {
    return parents;
  }
}
