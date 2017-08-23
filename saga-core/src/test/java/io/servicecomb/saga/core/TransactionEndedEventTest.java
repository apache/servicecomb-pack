
package io.servicecomb.saga.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TransactionEndedEventTest {
  
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
    TransactionEndedEvent transactionEndedEvent = new TransactionEndedEvent("123456", sagaRequestResponse.sagaRequest(), sagaRequestResponse.sagaResponse());
    Assert.assertTrue(transactionEndedEvent.json().equals(objectMapper.writeValueAsString(sagaRequestResponse)));
    System.out.println(transactionEndedEvent.json());
    Assert.assertFalse(isException);
  }

}
