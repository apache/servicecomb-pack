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

package io.servicecomb.saga.demo.payment;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class PaymentController {
  private int balance = 1000;

  @RequestMapping(value = "payments", method = POST, consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> pay(@RequestBody String customerId) {
    if ("anonymous".equals(customerId)) {
      return new ResponseEntity<>("No such customer with id " + customerId, FORBIDDEN);
    }

    if (balance < 800) {
      return ResponseEntity.badRequest().body("Not enough balance in account of customer " + customerId);
    }

    balance -= 800;
    return ResponseEntity.ok(String.format("Payment made for customer %s and remaining balance is %d",
        customerId,
        balance));
  }

  @RequestMapping(value = "payments", method = PUT, consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> refund(@RequestBody String customerId) {
    if ("anonymous".equals(customerId)) {
      return new ResponseEntity<>("No such customer with id " + customerId, FORBIDDEN);
    }

    balance += 800;
    return ResponseEntity.ok(String.format("Payment refunded for customer %s and remaining balance is %d",
        customerId,
        balance));
  }
}
