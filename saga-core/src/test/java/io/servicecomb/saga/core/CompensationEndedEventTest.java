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

public class CompensationEndedEventTest {
 private static final String requestAndResponse =
      
      "  { \"sagaRequest\":"
      + "  {\n"
      + "    \"id\": \"request-yyy\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"localhost:8090\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rest/yyy\",\n"
      + "      \"params\": {\n"
      + "        \"form\": {\n"
      + "          \"foo\": \"yyy\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"delete\",\n"
      + "      \"path\": \"/rest/yyy\",\n"
      + "      \"params\": {\n"
      + "        \"query\": {\n"
      + "          \"bar\": \"yyy\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  },\n"  
      + "   \"sagaResponse\": {\n"
      + "        \"statusCode\": \"200\",\n"
      + "        \"body\": \"test\"\n"
      + "    }\n"
      +"}";
  @Test
  public void json() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    SagaRequestResponse sagaRequestResponse = null;
    boolean isException = false;
    try {
      sagaRequestResponse = objectMapper.readValue(requestAndResponse, SagaRequestResponse.class);
    } catch (IOException e) {
      e.printStackTrace();
      isException = true; 
    }
    CompensationEndedEvent compensationEndedEvent = new CompensationEndedEvent("123456", sagaRequestResponse.sagaRequest(),sagaRequestResponse.sagaResponse());
    Assert.assertTrue(compensationEndedEvent.json().equals(objectMapper.writeValueAsString(sagaRequestResponse)));
    Assert.assertFalse(isException);
    Assert.assertTrue(compensationEndedEvent.json().contains("localhost:8090"));
    Assert.assertTrue(compensationEndedEvent.json().contains("200"));
  }
}
