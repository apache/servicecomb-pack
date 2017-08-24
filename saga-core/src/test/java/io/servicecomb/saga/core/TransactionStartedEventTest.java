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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.servicecomb.saga.core.application.interpreter.JsonSagaRequest;

public class TransactionStartedEventTest {
  private static final String requestX =
        "  {\n"
      + "    \"id\": \"request-xxx\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"localhost:8090\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/xxx\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"foo\": \"xxx\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/xxx\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"bar\": \"xxx\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n";
  
  @Test
  public void json() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    SagaRequest sagaRequest = null;
    boolean isException=false;
    try {
      sagaRequest = objectMapper.readValue(requestX, JsonSagaRequest.class);
    } catch (IOException e) {
      isException=true;
    }
    TransactionStartedEvent transactionStartedEventTest = new TransactionStartedEvent("123456", sagaRequest);
    Assert.assertTrue(transactionStartedEventTest.json().equals(objectMapper.writeValueAsString(sagaRequest)));
    Assert.assertFalse(isException);
    Assert.assertTrue(transactionStartedEventTest.json().contains("localhost:8090"));
    Assert.assertTrue(transactionStartedEventTest.json().contains("bar"));
  }

}
