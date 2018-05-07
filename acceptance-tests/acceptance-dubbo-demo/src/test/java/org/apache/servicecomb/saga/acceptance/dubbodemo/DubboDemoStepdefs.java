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

package org.apache.servicecomb.saga.acceptance.dubbodemo;

import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java8.En;
import io.restassured.response.Response;
import org.hamcrest.core.StringContains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

public class DubboDemoStepdefs implements En {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String ALPHA_REST_ADDRESS = "alpha.rest.address";
  private static final String SERVICEA_ADDRESS = "servicea.address";
  private static final String SERVICEB_ADDRESS = "serviceb.address";
  private static final String SERVICEC_ADDRESS = "servicec.address";
  private static final String INFO_SERVICE_URI = "info.service.uri";

  private static final Consumer<Map<String, String>[]> NO_OP_CONSUMER = (dataMap) -> {
  };

  public DubboDemoStepdefs() {
    Given("^ServiceA is up and running$", () -> {
      probe(System.getProperty(SERVICEA_ADDRESS));
    });

    And("^ServiceB is up and running$", () -> {
      probe(System.getProperty(SERVICEB_ADDRESS));
    });

    And("^ServiceC is up and running$", () -> {
      probe(System.getProperty(SERVICEC_ADDRESS));
    });

    And("^Alpha is up and running$", () -> {
      probe(System.getProperty(ALPHA_REST_ADDRESS));
    });

    When("^([A-Za-z]+) :: .*$", (invokeCode) -> {
      LOG.info("invokeCode: " + invokeCode);

      Response resp = given()
          .pathParam("invokeCode", invokeCode)
          .when()
          .get(System.getProperty(SERVICEA_ADDRESS) + "/serviceInvoke/{invokeCode}");
      LOG.info("response status code:" + resp.statusCode());
      resp.then().content(new StringContains("check result: true"));
    });

    Then("^Alpha records the following events$", (DataTable dataTable) -> {
      Consumer<Map<String, Object>[]> sortAndColumnStrippingConsumer = dataMap -> {
        //sort first
        Arrays.sort(dataMap, (o1, o2) -> {
          Integer id1 = (Integer)o1.get("surrogateId");
          Integer id2 = (Integer)o2.get("surrogateId");
          if(id1 == null || id2 == null) return 0;
          return id1.compareTo(id2);
        });
        //strip columns
        for (Map<String, Object> map : dataMap)
          map.keySet().retainAll(dataTable.topCells());
      };

      dataMatches(System.getProperty(ALPHA_REST_ADDRESS) + "/events", dataTable, sortAndColumnStrippingConsumer);
    });

    And("^(service[a-c]+) success update status$", (String serviceName, DataTable dataTable) -> {
      Consumer<Map<String, Object>[]> columnStrippingConsumer = dataMap -> {
        for (Map<String, Object> map : dataMap)
          map.keySet().retainAll(dataTable.topCells());
      };

      dataMatches(System.getProperty(SERVICEA_ADDRESS) + "/serviceInfo/"+serviceName, dataTable, columnStrippingConsumer);
    });
  }

  @Before
  public void cleanUp() {
    LOG.info("Cleaning up services");

    given()
            .when()
            .delete(System.getProperty(ALPHA_REST_ADDRESS) + "/events")
            .then()
            .statusCode(is(200));
  }

  @SuppressWarnings("unchecked")
  private void dataMatches(String address, DataTable dataTable, Consumer<Map<String, Object>[]> dataProcessor) {
    List<Map<String, Object>> expectedMaps = dataTable.asMaps(String.class, Object.class);
    List<Map<String, Object>> actualMaps = new ArrayList<>();

    await().atMost(8, SECONDS).until(() -> {
      actualMaps.clear();
      Collections.addAll(actualMaps, retrieveDataMaps(address, dataProcessor));

      return expectedMaps.size() == actualMaps.size();
    });

    if (expectedMaps.isEmpty() && actualMaps.isEmpty()) {
      return;
    }

    LOG.info("Retrieved data {} from service", actualMaps);
    dataTable.diff(DataTable.create(actualMaps));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object>[] retrieveDataMaps(String address, Consumer<Map<String, Object>[]> dataProcessor) {
    Map<String, Object>[] dataMap = given()
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
