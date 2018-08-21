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

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.saga.transports.TransportFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.servicecomb.saga.core.RestOperation;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.transports.RestTransport;

class JacksonRestOperation extends RestOperation implements TransportAware {

  @JsonIgnore
  private RestTransport transport;

  JacksonRestOperation(String path, String method, Map<String, Map<String, String>> params) {
    super(path, method, params);
  }

  @Override
  public JacksonRestOperation with(TransportFactory transport) {
    this.transport = transport.restTransport();
    return this;
  }

  @Override
  public SagaResponse send(String address) {
    return transport.with(address, path(), method(), params());
  }

  @Override
  public SagaResponse send(String address, SagaResponse response) {
    Map<String, Map<String, String>> updated = new HashMap<>(params());
    // This is not thread safe
    if (updated.get("form") == null) {
      updated.put("form", new HashMap<String, String>());
    }
    updated.get("form").put("response", response.body());

    return transport.with(
        address,
        path(),
        method(),
        updated);
  }
}
