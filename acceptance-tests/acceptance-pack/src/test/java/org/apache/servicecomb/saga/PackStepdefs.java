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

package org.apache.servicecomb.saga;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cucumber.api.DataTable;
import cucumber.api.java8.En;

public class PackStepdefs implements En {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public PackStepdefs() {
    Given("^Car Service is up and running$", () -> {
      probe(System.getProperty("car.service.address"));
    });

    And("^Hotel Service is up and running$", () -> {
      probe(System.getProperty("hotel.service.address"));
    });

    When("^User ([A-Za-z]+) requests to book ([0-9]+) cars and ([0-9]+) rooms$", (username, cars, rooms) -> {
      log.info("Received request from user {} to book {} cars and {} rooms", username, cars, rooms);

      given()
          .pathParam("name", username)
          .pathParam("rooms", rooms)
          .pathParam("cars", cars)
          .when()
          .post(System.getProperty("booking.service.address") + "/booking/{name}/{rooms}/{cars}")
          .then()
          .statusCode(is(200));
    });

    Then("^Alpha records the following events$", (DataTable dataTable) -> {
      List<Map<String, String>> maps = dataTable.asMaps(String.class, String.class);
      log.info("events {}", maps);
    });

    And("^Car Service contains the following booking orders$", (DataTable dataTable) -> {
      List<Map<String, String>> maps = dataTable.asMaps(String.class, String.class);
      log.info("car orders {}", maps);

      bookingsMatches(dataTable, "car.service.address");
    });

    And("^Hotel Service contains the following booking orders$", (DataTable dataTable) -> {
      List<Map<String, String>> maps = dataTable.asMaps(String.class, String.class);
      log.info("hotel orders {}", maps);

      bookingsMatches(dataTable, "hotel.service.address");
    });
  }

  @SuppressWarnings("unchecked")
  private void bookingsMatches(DataTable dataTable, String address) {
    Map<String, String>[] bookings = given()
        .when()
        .post(System.getProperty(address) + "/bookings")
        .then()
        .statusCode(is(200))
        .extract()
        .body()
        .as(Map[].class);

    dataTable.diff(Arrays.stream(bookings).collect(Collectors.toList()));
  }

  private void probe(String address) {
    given()
        .when()
        .post(address + "/info")
        .then()
        .statusCode(is(200));
  }
}
