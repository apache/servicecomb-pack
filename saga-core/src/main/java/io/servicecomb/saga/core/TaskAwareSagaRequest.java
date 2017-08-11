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

public class TaskAwareSagaRequest implements SagaRequest {

  private final String id;
  private final String serviceName;
  private final String type;
  private final Transaction transaction;
  private final Compensation compensation;
  private final String requestJson;


  public TaskAwareSagaRequest(String id, Transaction transaction, Compensation compensation, String requestJson) {
    this(id, "Saga", "nop", transaction, compensation, requestJson);
  }

  TaskAwareSagaRequest(String id,
      String serviceName,
      String type,
      Transaction transaction,
      Compensation compensation,
      String requestJson) {

    this.id = id;
    this.serviceName = serviceName;
    this.type = type;
    this.transaction = transaction;
    this.compensation = compensation;
    this.requestJson = requestJson;
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

  @Override
  public String type() {
    return type;
  }

  @Override
  public String json() {
    return requestJson;
  }
}
