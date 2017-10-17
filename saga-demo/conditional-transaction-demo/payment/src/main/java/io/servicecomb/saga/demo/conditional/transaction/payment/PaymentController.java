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

package io.servicecomb.saga.demo.conditional.transaction.payment;

import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class PaymentController {
  private static final int PRICE = 400;
  private static final int UPGRADE_THRESHOLD = 1000;

  private final Map<String, Integer> customerPurchases = new HashMap<>(singletonMap("mike", 0));

  @RequestMapping(value = "payment", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> purchase(@RequestParam String customerId) {
    if (!customerPurchases.containsKey(customerId)) {
      return new ResponseEntity<>("No such customer with id " + customerId, FORBIDDEN);
    }

    customerPurchases.compute(customerId, (id, purchases) -> purchases + PRICE);

    if (isUpgradable(customerId)) {
      return ResponseEntity.ok(String.format("{\n"
              + "  \"body\": \"Payment made with id %s for customer %s\"\n"
              + "}",
          UUID.randomUUID().toString(),
          customerId));
    }

    return ResponseEntity.ok(String.format("{\n"
            + "  \"body\": \"Payment made with id %s for customer %s\",\n"
            + "  \"sagaChildren\": [\"inventory\"] \n"
            + "}",
        UUID.randomUUID().toString(),
        customerId));
  }

  @RequestMapping(value = "payment", method = PUT, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> refund(@RequestParam String customerId) {
    if (!customerPurchases.containsKey(customerId)) {
      return new ResponseEntity<>("No such customer with id " + customerId, FORBIDDEN);
    }

    return ResponseEntity.ok(String.format("Payment refunded with id %s for customer %s",
        UUID.randomUUID().toString(),
        customerId));
  }

  private boolean isUpgradable(String customerId) {
    return customerPurchases.get(customerId) >= UPGRADE_THRESHOLD;
  }
}
