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

package io.servicecomb.saga.demo.tests;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.Is.is;

import org.junit.BeforeClass;
import org.junit.Test;

public class DemoIT {

  private static final String requests = "[\n"
      + "  {\n"
      + "    \"id\": \"request-car\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"car.servicecomb.io:8080\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/rentals\",\n"
      + "      \"params\": {\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"customerId\\\": \\\"mike\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"put\",\n"
      + "      \"path\": \"/rentals\",\n"
      + "      \"params\": {\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"customerId\\\": \\\"mike\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  },\n"
      + "  {\n"
      + "    \"id\": \"request-hotel\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"hotel.servicecomb.io:8080\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/reservations\",\n"
      + "      \"params\": {\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"customerId\\\": \\\"mike\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"put\",\n"
      + "      \"path\": \"/reservations\",\n"
      + "      \"params\": {\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"customerId\\\": \\\"mike\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  },\n"
      + "  {\n"
      + "    \"id\": \"request-flight\",\n"
      + "    \"type\": \"rest\",\n"
      + "    \"serviceName\": \"flight.servicecomb.io:8080\",\n"
      + "    \"transaction\": {\n"
      + "      \"method\": \"post\",\n"
      + "      \"path\": \"/bookings\",\n"
      + "      \"params\": {\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"customerId\\\": \\\"mike\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    \"compensation\": {\n"
      + "      \"method\": \"put\",\n"
      + "      \"path\": \"/bookings\",\n"
      + "      \"params\": {\n"
      + "        \"json\": {\n"
      + "          \"body\": \"{ \\\"customerId\\\": \\\"mike\\\" }\"\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "]\n";

  private static String sagaAddress;

  @BeforeClass
  public static void setUp() throws Exception {
    sagaAddress = System.getProperty("saga.address");
  }

  @Test
  public void ableToSendRequestsToServicesThroughSaga() {
    given()
        .contentType(JSON)
        .body(requests)
        .when()
        .post(sagaAddress + "/requests/")
        .then()
        .body(is("success"));
  }
}
