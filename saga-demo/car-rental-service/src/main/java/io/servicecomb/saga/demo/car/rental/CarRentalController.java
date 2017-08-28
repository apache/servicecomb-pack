/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.demo.car.rental;

import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class CarRentalController {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Set<String> customers = new HashSet<>(asList("mike", "snail"));
  private int delay = 60;

  @RequestMapping(value = "rentals", method = POST, consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> rent(@RequestBody Customer customer) {
    log.info("Received car rental request from customer {}", customer.customerId);
    if (!customers.contains(customer.customerId)) {
      log.info("No such customer {}", customer.customerId);
      return new ResponseEntity<>("No such customer with id " + customer.customerId, FORBIDDEN);
    }

    if ("snail".equals(customer.customerId)) {
      try {
        log.info("Encountered extremely slow customer {}", customer.customerId);
        int timeout = delay;
        delay = 0;
        TimeUnit.SECONDS.sleep(timeout);
        log.info("Finally served the extremely slow customer {}", customer.customerId);
      } catch (InterruptedException e) {
        return new ResponseEntity<>("Interrupted", INTERNAL_SERVER_ERROR);
      }
    }

    return ResponseEntity.ok(String.format("Car rented with id %s for customer %s",
        UUID.randomUUID().toString(),
        customer.customerId));
  }

  @RequestMapping(value = "rentals", method = PUT, consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> cancel(@RequestBody Customer customer) {
    if (!customers.contains(customer.customerId)) {
      return new ResponseEntity<>("No such customer with id " + customer.customerId, FORBIDDEN);
    }

    return ResponseEntity.ok(String.format("Car rental cancelled with id %s for customer %s",
        UUID.randomUUID().toString(),
        customer.customerId));
  }

  private static class Customer {
    public String customerId;
  }
}
