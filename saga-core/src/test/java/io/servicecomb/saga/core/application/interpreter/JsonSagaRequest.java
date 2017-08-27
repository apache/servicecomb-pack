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
import io.servicecomb.saga.core.SagaRequestImpl;

class JsonSagaRequest extends SagaRequestImpl {

  @JsonCreator
  public JsonSagaRequest(
      @JsonProperty("id") String id,
      @JsonProperty("serviceName") String serviceName,
      @JsonProperty("type") String type,
      @JsonProperty("transaction") JsonTransaction transaction,
      @JsonProperty("compensation") JsonCompensation compensation,
      @JsonProperty("parents") String[] parents) {

    super(id, serviceName, type, transaction, compensation, parents);
  }
}
