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

import static io.servicecomb.saga.core.Fallback.NOP_FALLBACK;
import static io.servicecomb.saga.core.SagaTask.SAGA_REQUEST_TASK;

import java.util.Arrays;

public class SagaRequestImpl implements SagaRequest {

  private final String id;
  private final String serviceName;
  private final String type;
  private final Transaction transaction;
  private final Compensation compensation;
  private final String[] parents;
  private final Fallback fallback;
  private final int failRetryDelayMilliseconds;

  public SagaRequestImpl(
      String id,
      String serviceName,
      String type,
      Transaction transaction,
      Compensation compensation,
      Fallback fallback,
      String[] parents,
      int failRetryDelayMilliseconds) {

    this.id = id;
    this.serviceName = serviceName;
    this.type = type;
    this.transaction = transaction;
    this.compensation = compensation;
    this.fallback = fallback;
    this.failRetryDelayMilliseconds = failRetryDelayMilliseconds <= 0 ? 50 : failRetryDelayMilliseconds;
    // TODO: 2017/10/21 set parent to root when null
    this.parents = parents == null ? new String[0] : parents;
  }

  public SagaRequestImpl(
      String id,
      String serviceName,
      String type,
      Transaction transaction,
      Compensation compensation,
      Fallback fallback,
      String[] parents) {
    this(id, serviceName, type, transaction, compensation, fallback, parents, 0);
  }

  public SagaRequestImpl(
      String id,
      String serviceName,
      String type,
      Transaction transaction,
      Compensation compensation,
      Fallback fallback) {
    this(id, serviceName, type, transaction, compensation, fallback, new String[0]);
  }

  public SagaRequestImpl(
      String id,
      String serviceName,
      String type,
      Transaction transaction,
      Compensation compensation) {
    this(id, serviceName, type, transaction, compensation, NOP_FALLBACK, new String[0]);
  }

  public SagaRequestImpl(
      String id,
      String serviceName,
      String type,
      Transaction transaction,
      Compensation compensation,
      String[] parents) {
    this(id, serviceName, type, transaction, compensation, NOP_FALLBACK, parents);
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
  public Fallback fallback() {
    return fallback;
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
  public String task() {
    return SAGA_REQUEST_TASK;
  }

  @Override
  public String[] parents() {
    return parents;
  }

  @Override
  public int failRetryDelayMilliseconds() {
    return failRetryDelayMilliseconds;
  }

  @Override
  public String toString() {
    return "SagaRequest{" +
        "id='" + id + '\'' +
        ", serviceName='" + serviceName + '\'' +
        ", type='" + type + '\'' +
        ", transaction=" + transaction +
        ", compensation=" + compensation +
        ", fallback=" + fallback +
        ", parents=" + Arrays.toString(parents) +
        '}';
  }
}
