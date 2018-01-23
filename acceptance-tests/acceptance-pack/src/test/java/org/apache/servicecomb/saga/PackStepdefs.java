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

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cucumber.api.DataTable;
import cucumber.api.java8.En;

public class PackStepdefs implements En {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public PackStepdefs() {
    Given("^Car Service is up and running$", () -> {
    });

    And("^Hotel Service is up and running$", () -> {
    });

    When("^User ([A-Za-z]+) requests to book ([0-9]+) cars and ([0-9]+) rooms$", (username, cars, rooms) -> {
      log.info("Received request from user {} to book {} cars and {} rooms", username, cars, rooms);
    });

    Then("^Alpha records the following events$", (DataTable dataTable) -> {
      List<Map<String, String>> maps = dataTable.asMaps(String.class, String.class);
      log.info("events {}", maps);
    });

    And("^Car Service contains the following booking orders$", (DataTable dataTable) -> {
      List<Map<String, String>> maps = dataTable.asMaps(String.class, String.class);
      log.info("car orders {}", maps);
    });
    And("^Hotel Service contains the following booking orders$", (DataTable dataTable) -> {
      List<Map<String, String>> maps = dataTable.asMaps(String.class, String.class);
      log.info("hotel orders {}", maps);
    });
  }
}
