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

package org.apache.servicecomb.saga.demo.car.rental;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Controller
@RequestMapping("/")
@RestSchema(schemaId = "car-endpoint")
public class CarRentalController {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Set<String> customers = new HashSet<>(asList("mike", "snail"));
  private int delay = 60;

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "authenticated user"),
      @ApiResponse(code = 403, response = String.class, message = "unauthenticated user")
  })
  @RequestMapping(value = "rentals", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> rent(@RequestAttribute String customerId) {
    log.info("Received car rental request from customer {}", customerId);
    if (!customers.contains(customerId)) {
      log.info("No such customer {}", customerId);
      throw new InvocationException(FORBIDDEN, "No such customer with id " + customerId);
    }

    if ("snail".equals(customerId)) {
      try {
        log.info("Encountered extremely slow customer {}", customerId);
        int timeout = delay;
        delay = 0;
        TimeUnit.SECONDS.sleep(timeout);
        log.info("Finally served the extremely slow customer {}", customerId);
      } catch (InterruptedException e) {
        return new ResponseEntity<>("Interrupted", INTERNAL_SERVER_ERROR);
      }
    }

    return ResponseEntity.ok(String.format("{\n"
            + "  \"body\": \"Car rented with id %s for customer %s\"\n"
            + "}",
        UUID.randomUUID().toString(),
        customerId));
  }

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "authenticated user"),
      @ApiResponse(code = 403, response = String.class, message = "unauthenticated user")
  })
  @RequestMapping(value = "rentals", method = PUT, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> cancel(@RequestAttribute String customerId) {
    if (!customers.contains(customerId)) {
      throw new InvocationException(FORBIDDEN, "No such customer with id " + customerId);
    }

    return ResponseEntity.ok(String.format("Car rental cancelled with id %s for customer %s",
        UUID.randomUUID().toString(),
        customerId));
  }
}
