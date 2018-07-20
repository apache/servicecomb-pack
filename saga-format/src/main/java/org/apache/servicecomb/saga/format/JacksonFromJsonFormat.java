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

import java.io.IOException;

import org.apache.servicecomb.saga.transports.TransportFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.servicecomb.saga.core.SagaDefinition;
import org.apache.servicecomb.saga.core.SagaException;
import org.apache.servicecomb.saga.core.application.interpreter.FromJsonFormat;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class JacksonFromJsonFormat implements FromJsonFormat<SagaDefinition> {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final TransportFactory transportFactory;

  public JacksonFromJsonFormat(TransportFactory transportFactory) {
    this.transportFactory = transportFactory;
  }

  @Segment(name = "fromJson", category = "application", library = "kamon")
  @Override
  public SagaDefinition fromJson(String requestJson) {
    try {
      JsonSagaDefinition definition = objectMapper.readValue(requestJson, JsonSagaDefinition.class);

      for (JsonSagaRequest request : definition.requests()) {
        request.with(transportFactory);
      }

      return definition;
    } catch (IOException e) {
      throw new SagaException("Failed to interpret JSON " + requestJson, e);
    }
  }
}
