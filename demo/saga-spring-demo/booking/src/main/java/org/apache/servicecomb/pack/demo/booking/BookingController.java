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

package org.apache.servicecomb.pack.demo.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class BookingController {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${car.service.address:http://car.servicecomb.io:8080}")
  private String carServiceUrl;

  @Value("${hotel.service.address:http://hotel.servicecomb.io:8080}")
  private String hotelServiceUrl;

  @Autowired
  private RestTemplate template;

  @SagaStart
  @PostMapping("/booking/{name}/{rooms}/{cars}")
  public String order(@PathVariable String name, @PathVariable Integer rooms,
      @PathVariable Integer cars) throws Throwable {

    if (cars < 0) {
      throw new Exception("The cars order quantity must be greater than 0");
    }

    template.postForEntity(
        carServiceUrl + "/order/{name}/{cars}",
        null, String.class, name, cars);

    postCarBooking();

    if (rooms < 0) {
      throw new Exception("The rooms order quantity must be greater than 0");
    }

    template.postForEntity(
        hotelServiceUrl + "/order/{name}/{rooms}",
        null, String.class, name, rooms);

    postBooking();

    return name + " booking " + rooms + " rooms and " + cars + " cars OK";
  }

  // This method is used by the byteman to inject exception here
  private void postCarBooking() throws Throwable {

  }

  // This method is used by the byteman to inject the faults such as the timeout or the crash
  private void postBooking() throws Throwable {

  }

  // This method is used by the byteman trigger shutdown the master node in the Alpha server cluster
  private void alphaMasterShutdown() {
    String alphaRestAddress = System.getenv("alpha.rest.address");
    LOG.info("alpha.rest.address={}", alphaRestAddress);
    List<String> addresss = Arrays.asList(alphaRestAddress.split(","));

    addresss.stream().filter(address -> {
      // use the actuator alpha endpoint to find the alpha master node
      try {
        ResponseEntity<String> responseEntity = template
            .getForEntity(address + "/actuator/alpha", String.class);
        ObjectMapper mapper = new ObjectMapper();
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
          String json = responseEntity.getBody();
          Map<String, String> map = mapper.readValue(json, Map.class);
          if (map.get("nodeType").equalsIgnoreCase("MASTER")) {
            return true;
          }
        }
      } catch (Exception ex) {
        LOG.error("", ex);
      }
      return false;
    }).forEach(address -> {
      // call shutdown endpoint to shutdown the alpha master node
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity request = new HttpEntity(headers);
      ResponseEntity<String> responseEntity = template
          .postForEntity(address + "/actuator/shutdown", request, String.class);
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        LOG.info("Alpah master node {} shutdown", address);
      }
    });
  }
}
