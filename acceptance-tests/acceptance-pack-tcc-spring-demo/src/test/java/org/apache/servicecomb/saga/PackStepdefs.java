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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.response.Response;
import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java8.En;

public class PackStepdefs implements En {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String ALPHA_REST_ADDRESS = "alpha.rest.address";
  private static final String INVENTORY_SERVICE_ADDRESS = "inventory.service.address";
  private static final String PAYMENT_SERVICE_ADDRESS = "payment.service.address";
  private static final String ORDERING_SERVICE_ADDRESS = "ordering.service.address";
  private static final String INVENTORY_ORDERS_URI = "/orderings";
  private static final String PAYMENT_ORDERS_URI = "/transactions";
  private static final String INFO_SERVICE_URI = "info.service.uri";
  private static final String[] addresses = {INVENTORY_SERVICE_ADDRESS, PAYMENT_SERVICE_ADDRESS};
  private static final String[] uris = {INVENTORY_ORDERS_URI, PAYMENT_ORDERS_URI};

  private static final Consumer<Map<String, String>[]> NO_OP_CONSUMER = (dataMap) -> {
  };

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
      // Need to wait for a while to let the confirm or cannel command finished.
      Thread.sleep(2000);
    });

    //TODO need to check the events from the alpha
    /*Then("^Alpha records the following events$", (DataTable dataTable) -> {
      Consumer<Map<String, String>[]> columnStrippingConsumer = dataMap -> {
        for (Map<String, String> map : dataMap)
          map.keySet().retainAll(dataTable.topCells());
      };

      dataMatches(System.getProperty(ALPHA_REST_ADDRESS) + "/events", dataTable, columnStrippingConsumer);
    });*/

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

    /*given()
        .when()
        .delete(System.getProperty(ALPHA_REST_ADDRESS) + "/events")
        .then()
        .statusCode(is(200));*/

  }

  @SuppressWarnings("unchecked")
  private void dataMatches(String address, DataTable dataTable, Consumer<Map<String, String>[]> dataProcessor) {
    List<Map<String, String>> expectedMaps = dataTable.asMaps(String.class, String.class);
    List<Map<String, String>> actualMaps = new ArrayList<>();

    await().atMost(5, SECONDS).until(() -> {
      actualMaps.clear();
      Collections.addAll(actualMaps, retrieveDataMaps(address, dataProcessor));
      // write the log if the Map size is not same
      boolean result = expectedMaps.size() == actualMaps.size();
      if (!result) {
        LOG.warn("The response message size is not we expected. ExpectedMap size is {},  ActualMap size is {}", expectedMaps.size(), actualMaps.size());
      }
      return expectedMaps.size() == actualMaps.size();
    });

    if (expectedMaps.isEmpty() && actualMaps.isEmpty()) {
      return;
    }

    LOG.info("Retrieved data {} from service", actualMaps);
    dataTable.diff(DataTable.create(actualMaps));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String>[] retrieveDataMaps(String address, Consumer<Map<String, String>[]> dataProcessor) {
    Map<String, String>[] dataMap = given()
        .when()
        .get(address)
        .then()
        .statusCode(is(200))
        .extract()
        .body()
        .as(Map[].class);

    dataProcessor.accept(dataMap);
    return dataMap;
  }

  private void probe(String address) {
    String infoURI = System.getProperty(INFO_SERVICE_URI);
    if (isEmpty(infoURI)) {
      infoURI = "/info";
    }
    LOG.info("The info service uri is " + infoURI);
    probe(address, infoURI);
  }

  private void probe(String address, String infoURI) {
    LOG.info("Connecting to service address {}", address);
    given()
        .when()
        .get(address + infoURI)
        .then()
        .statusCode(is(200));
  }

}
