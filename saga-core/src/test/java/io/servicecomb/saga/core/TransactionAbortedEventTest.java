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

public class TransactionAbortedEventTest {
  
  private static final String singleRequestY1 =
      "  {\n"
          + "    \"id\": \"request-yyy-1\",\n"
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
          + "  }\n"
          ;
  private static final String requestY2AndException =
      
      "  { \"sagaRequest\":"
          +          singleRequestY1+","+"\n"  
          + "   \"exception\": \n";
  
 

  @Test
  public void json() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    SagaRequestException sagaRequestException = null;
    boolean isException = false;

    try {
      String temp = objectMapper.writeValueAsString(new IllegalArgumentException("exception test"));

      sagaRequestException = objectMapper.readValue(requestY2AndException + temp + "}", SagaRequestException.class);
    } catch (IOException e) {
      e.printStackTrace();
      isException = true;
    }
    TransactionAbortedEvent transactionAbortedEvent =
        new TransactionAbortedEvent("123456", sagaRequestException.sagaRequest(), sagaRequestException.exception());
    Assert.assertTrue(transactionAbortedEvent.json().equals(objectMapper.writeValueAsString(sagaRequestException)));
    System.out.println(transactionAbortedEvent.json());
    Assert.assertTrue(transactionAbortedEvent.json().contains("exception test"));
    Assert.assertTrue(transactionAbortedEvent.json().contains("localhost:8090"));
    Assert.assertFalse(isException);
  }
}
