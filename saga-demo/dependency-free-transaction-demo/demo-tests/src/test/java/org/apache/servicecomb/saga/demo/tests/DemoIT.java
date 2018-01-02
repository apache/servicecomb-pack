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

package org.apache.servicecomb.saga.demo.tests;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.TEXT;
import static org.hamcrest.core.Is.is;

import org.junit.BeforeClass;
import org.junit.Test;

public class DemoIT {

  private static final String requests = "{\n"
      + "  \"policy\": \"BackwardRecovery\",\n"
      + "  \"requests\": [\n"
      + "    {\n"
      + "      \"id\": \"request-car\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"car-rental-service\",\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/rentals\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"method\": \"put\",\n"
      + "        \"path\": \"/rentals\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": \"request-hotel\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"hotel-reservation-service\",\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/reservations\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"method\": \"put\",\n"
      + "        \"path\": \"/reservations\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": \"request-flight\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"flight-booking-service\",\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/bookings\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"method\": \"put\",\n"
      + "        \"path\": \"/bookings\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      }\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": \"request-payment\",\n"
      + "      \"type\": \"rest\",\n"
      + "      \"serviceName\": \"saga-crossapp:payment-service\",\n"
      + "      \"parents\": [\n"
      + "        \"request-car\",\n"
      + "        \"request-flight\",\n"
      + "        \"request-hotel\"\n"
      + "      ],\n"
      + "      \"transaction\": {\n"
      + "        \"method\": \"post\",\n"
      + "        \"path\": \"/payments\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      },\n"
      + "      \"compensation\": {\n"
      + "        \"method\": \"put\",\n"
      + "        \"path\": \"/payments\",\n"
      + "        \"params\": {\n"
      + "          \"form\": {\n"
      + "            \"customerId\": \"mike\"\n"
      + "          }\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  ]\n"
      + "}\n";

  private static String sagaAddress;

  @BeforeClass
  public static void setUp() throws Exception {
    sagaAddress = System.getProperty("saga.address");
  }

  @Test
  public void ableToSendRequestsToServicesThroughSaga() {
    given()
        .contentType(TEXT)
        .body(requests)
        .when()
        .post(sagaAddress + "/requests")
        .then()
        .statusCode(is(200))
        .body(is("success"));
  }
}
