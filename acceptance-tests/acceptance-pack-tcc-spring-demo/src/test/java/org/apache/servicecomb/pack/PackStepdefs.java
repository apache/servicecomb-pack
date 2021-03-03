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

package org.apache.servicecomb.pack;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.response.Response;
import cucumber.api.DataTable;
import cucumber.api.java.After;


public class PackStepdefs extends StepDefSupport {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String ALPHA_REST_ADDRESS = "alpha.rest.address";
  private static final String INVENTORY_SERVICE_ADDRESS = "inventory.service.address";
  private static final String PAYMENT_SERVICE_ADDRESS = "payment.service.address";
  private static final String ORDERING_SERVICE_ADDRESS = "ordering.service.address";
  private static final String INVENTORY_ORDERS_URI = "/orderings";
  private static final String PAYMENT_ORDERS_URI = "/transactions";
  private static final String[] addresses = {INVENTORY_SERVICE_ADDRESS, PAYMENT_SERVICE_ADDRESS};
  private static final String[] uris = {INVENTORY_ORDERS_URI, PAYMENT_ORDERS_URI};

  public PackStepdefs() {
    Given("^Inventory Service is up and running$", () -> {
      probe(System.getProperty(INVENTORY_SERVICE_ADDRESS));
    });

    And("^Payment Service is up and running$", () -> {
      probe(System.getProperty(PAYMENT_SERVICE_ADDRESS));
    });

    And("^Ordering Service is up and running$", () -> {
      probe(System.getProperty(ORDERING_SERVICE_ADDRESS));
    });

    And("^Alpha is up and running$", () -> {
      probe(System.getProperty(ALPHA_REST_ADDRESS));
    });

    
    When("^User ([A-Za-z]+) requests to order ([0-9]+) units of ([A-Za-z]+) with unit price ([0-9]+) (success|fail)$",
        (userName, productUnits, productName, unitPrice, result) -> {
      LOG.info("Received request from user {} to order {} units of {}  with unit price {}", userName, productUnits, productName, unitPrice);

      Response resp = given()
          .pathParam("userName", userName)
          .pathParam("productName", productName)
          .pathParam("productUnits", productUnits)
          .pathParam("unitPrice", unitPrice)
          .when()
          .post(System.getProperty(ORDERING_SERVICE_ADDRESS) + "/ordering/order/{userName}/{productName}/{productUnits}/{unitPrice}");
      if (result.equals("success")) {
        resp.then().statusCode(is(200));
      } else if (result.equals("fail")) {
        resp.then().statusCode(is(500));
      }
      // Need to wait for a while to let the confirm or cancel command finished.
      Thread.sleep(2000);
    });
    
    Then("^Alpha records the following events$", (DataTable dataTable) -> {
      Consumer<Map<String, String>[]> columnStrippingConsumer = dataMap -> {
        for (Map<String, String> map : dataMap)
          map.keySet().retainAll(dataTable.topCells());
      };
      // SCB-2201 Here we don't check the order of tcc event
      dataMatches(System.getProperty(ALPHA_REST_ADDRESS) + "/tcc/events", dataTable, columnStrippingConsumer, false);
    });

    Then("^Inventory Service contains the following booking orders$", (DataTable dataTable) -> {
      dataMatches(System.getProperty(INVENTORY_SERVICE_ADDRESS) + INVENTORY_ORDERS_URI, dataTable, NO_OP_CONSUMER);
    });

    And("^Payment Service contains the following booking orders$", (DataTable dataTable) -> {
      dataMatches(System.getProperty(PAYMENT_SERVICE_ADDRESS) + PAYMENT_ORDERS_URI, dataTable, NO_OP_CONSUMER);
    });
  }

  @After
  public void cleanUp() {
    LOG.info("Cleaning up services");
    for (int i = 0; i < addresses.length; i++) {
      given()
          .when()
          .delete(System.getProperty(addresses[i]) + uris[i])
          .then()
          .statusCode(is(200));
    }

    given()
        .when()
        .delete(System.getProperty(ALPHA_REST_ADDRESS) + "/tcc/events")
        .then()
        .statusCode(is(200));

  }
}
