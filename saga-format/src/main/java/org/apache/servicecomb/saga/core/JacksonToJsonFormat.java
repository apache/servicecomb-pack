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

package org.apache.servicecomb.saga.core;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
public class JacksonToJsonFormat implements ToJsonFormat {


  private final ObjectMapper objectMapper = new ObjectMapper();

  public JacksonToJsonFormat() {
    objectMapper.setVisibility(
        objectMapper.getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(ANY)
            .withGetterVisibility(NONE)
            .withSetterVisibility(NONE)
            .withCreatorVisibility(NONE));

  }

  @Segment(name = "toJson", category = "application", library = "kamon")
  @Override
  public String toJson(SagaRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      throw new SagaException("Failed to serialize request to JSON: " + request, e);
    }
  }

  @Segment(name = "toJson", category = "application", library = "kamon")
  @Override
  public String toJson(SagaRequest request, SagaResponse response) {
    try {
      return objectMapper.writeValueAsString(new SagaRequestContext(request, response));
    } catch (JsonProcessingException e) {
      throw new SagaException("Failed to serialize request & response to JSON: " + request + ", " + response, e);
    }
  }
}
