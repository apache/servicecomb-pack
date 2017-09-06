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

package io.servicecomb.saga.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.servicecomb.saga.core.Compensation;
import io.servicecomb.saga.core.RestOperation;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.transports.RestTransport;
import java.util.Map;

public class JacksonRestCompensation extends RestOperation implements Compensation {

  @JsonIgnore
  private RestTransport transport;

  @JsonCreator
  public JacksonRestCompensation(
      @JsonProperty("path") String path,
      @JsonProperty("method") String method,
      @JsonProperty("params") Map<String, Map<String, String>> params) {
    super(path, method, params);
  }

  JacksonRestCompensation with(RestTransport transport) {
    this.transport = transport;
    return this;
  }

  @Override
  public SagaResponse send(String address) {
    return transport.with(address, path(), method(), params());
  }
}
