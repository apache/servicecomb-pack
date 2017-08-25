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

package io.servicecomb.saga.demo.hotel.reservation;

import static java.util.Collections.singleton;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HotelReservationController {

  private final Set<String> customers = singleton("mike");

  @RequestMapping(value = "reservations", method = POST, consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> reserve(@RequestBody Customer customer) {
    if (!customers.contains(customer.customerId)) {
      return new ResponseEntity<>("No such customer with id " + customer.customerId, FORBIDDEN);
    }

    return ResponseEntity.ok(String.format("Hotel reserved with id %s for customer %s",
        UUID.randomUUID().toString(),
        customer.customerId));
  }

  @RequestMapping(value = "reservations", method = PUT, consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> cancel(@RequestBody Customer customer) {
    if (!customers.contains(customer.customerId)) {
      return new ResponseEntity<>("No such customer with id " + customer.customerId, FORBIDDEN);
    }

    return ResponseEntity.ok(String.format("Hotel reservation cancelled with id %s for customer %s",
        UUID.randomUUID().toString(),
        customer.customerId));
  }

  private static class Customer {
    public String customerId;
  }
}
