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

package org.apache.servicecomb.pack.acceptance.dubbodemo;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.servicecomb.pack.StepDefSupport;
import org.hamcrest.core.StringContains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.response.Response;
import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java8.En;

public class DubboDemoStepdefs extends StepDefSupport {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String ALPHA_REST_ADDRESS = "alpha.rest.address";

  private static final String SERVICEA_ADDRESS = "servicea.address";

  private static final String SERVICEB_ADDRESS = "serviceb.address";

  private static final String SERVICEC_ADDRESS = "servicec.address";


  public DubboDemoStepdefs() {
    Given("^ServiceA is up and running$", () -> probe(System.getProperty(SERVICEA_ADDRESS)));

    And("^ServiceB is up and running$", () -> probe(System.getProperty(SERVICEB_ADDRESS)));

    And("^ServiceC is up and running$", () -> probe(System.getProperty(SERVICEC_ADDRESS)));

    And("^Alpha is up and running$", () -> probe(System.getProperty(ALPHA_REST_ADDRESS)));

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
      Consumer<Map<String, String>[]> sortAndColumnStrippingConsumer = dataMaps -> {
        //blur match: service for sagaEndedEvent may be unable to que
        for (Map<String, String> dataMap : dataMaps) {
          if (dataMap.values().contains("SagaEndedEvent")) {
            for (String key : dataMap.keySet()) {
              if ("SagaEndedEvent".equals(dataMap.get(key))) {
                dataMap.put("serviceName", "*");
                break;
              }
            }
          }
        }
        //strip columns
        for (Map<String, String> map : dataMaps) {
          map.keySet().retainAll(dataTable.topCells());
        }
      };

      dataMatches(System.getProperty(ALPHA_REST_ADDRESS) + "/saga/events", dataTable, sortAndColumnStrippingConsumer, false);
    });

    And("^(service[a-c]+) success update status$", (String serviceName, DataTable dataTable) -> {
      Consumer<Map<String, String>[]> columnStrippingConsumer = dataMap -> {
        for (Map<String, String> map : dataMap) {
          map.keySet().retainAll(dataTable.topCells());
        }
      };

      dataMatches(System.getProperty(SERVICEA_ADDRESS) + "/serviceInfo/" + serviceName, dataTable,
          columnStrippingConsumer, false);
    });
  }

  @Before
  public void cleanUp() {
    LOG.info("Cleaning up services");

    given()
        .when()
        .delete(System.getProperty(ALPHA_REST_ADDRESS) + "/saga/events")
        .then()
        .statusCode(is(200));
  }


}
