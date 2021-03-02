package org.apache.servicecomb.pack;/*
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

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.byteman.agent.submit.Submit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cucumber.api.DataTable;
import cucumber.api.java8.En;

public class StepDefSupport implements En {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String INFO_SERVICE_URI = "info.service.uri";
  protected static final Map<String, Submit> submits = new HashMap<>();
  protected static final Consumer<Map<String, String>[]> NO_OP_CONSUMER = (dataMap) -> {
  };

  protected void dataMatches(String address, DataTable dataTable, Consumer<Map<String, String>[]> dataProcessor) {
    dataMatches(address, dataTable, dataProcessor, true);
  }

  protected void dataMatches(String address, DataTable dataTable, Consumer<Map<String, String>[]> dataProcessor, boolean checkOrder) {
    List<Map<String, String>> expectedMaps = dataTable.asMaps(String.class, String.class);
    List<Map<String, String>> actualMaps = new ArrayList<>();

    await().atMost(30, SECONDS).until(() -> {
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
    if (checkOrder) {
      dataTable.diff(DataTable.create(actualMaps));
    } else {
      dataTable.unorderedDiff(DataTable.create(actualMaps));
    }
  }

  @SuppressWarnings("unchecked")
  protected Map<String, String>[] retrieveDataMaps(String address, Consumer<Map<String, String>[]> dataProcessor) {
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

  protected void probe(String address) {
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

  protected Submit getBytemanSubmit(String service) {
    if (submits.containsKey(service)) {
      return submits.get(service);
    } else {
      String address = System.getProperty("byteman.address");
      String port = System.getProperty(service.toLowerCase() + ".byteman.port");
      Submit bm = new Submit(address, Integer.parseInt(port));
      submits.put(service, bm);
      return bm;
    }
  }

  protected void probeAlphaMaster(AlpahClusterAddress alphaClusterAddress) {
    LOG.info("Check to Alpha Master server");
    assertNotNull(alphaClusterAddress.getMasterAddress());
  }

  class AlpahClusterAddress {

    private List<String> addresss;

    private int maxRetry=6;

    AlpahClusterAddress(String address) {
      this.addresss = Arrays.asList(address.split(","));
    }

    public List<String> getAddresss() {
      return addresss;
    }

    // get the master node in the alpha server cluster
    public String getMasterAddress() {
      Predicate<String> matchMasterNode =
          endpoint -> matches(endpoint::toString, ofNullable("MASTER"));
      Optional<String> masterAddress = Optional.empty();
      int retryCounter = 0;
      while(!masterAddress.isPresent() && retryCounter<maxRetry){
        masterAddress = addresss.stream().filter(matchMasterNode).findAny();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        retryCounter++;
      }
      if(masterAddress.isPresent()){
        return masterAddress.get();
      }else{
        throw new RuntimeException("Max retries exception!");
      }
    }

    private <T> boolean matches(Supplier<T> supplier, Optional<String> value) {
      try{
        String nodeType = given().get(supplier.get() + "/alpha/api/v1/metrics").jsonPath().getString("nodeType");
        LOG.info("Check alpha server {} nodeType is {}",supplier.get(),nodeType);
        if (value.get().equalsIgnoreCase(nodeType)) {
          return true;
        } else {
          return false;
        }
      }catch (Exception ex){
        LOG.info("Check alpha server {} nodeType fail, {}",supplier.get(),ex.getMessage());
        return false;
      }
    }
  }
}
