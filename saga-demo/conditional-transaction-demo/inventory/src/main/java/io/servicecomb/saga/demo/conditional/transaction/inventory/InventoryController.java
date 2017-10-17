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

package io.servicecomb.saga.demo.conditional.transaction.inventory;

import static java.util.Collections.singleton;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class InventoryController {
  private static final int FETCH_THRESHOLD = 10;

  private final Set<String> customerPurchases = singleton("mike");
  private int stock = 11;

  @RequestMapping(value = "inventory", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> dispatch(@RequestParam String customerId) {
    if (!customerPurchases.contains(customerId)) {
      return new ResponseEntity<>("No such customer with id " + customerId, FORBIDDEN);
    }

    stock--;
    if (isStockShort()) {
      return response(customerId, "");
    }

    return response(customerId,",  \"sagaChildren\": [\"none\"] \n");
  }

  @RequestMapping(value = "inventory", method = PUT, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> recall(@RequestParam String customerId) {
    if (!customerPurchases.contains(customerId)) {
      return new ResponseEntity<>("No such customer with id " + customerId, FORBIDDEN);
    }

    return ResponseEntity.ok(String.format("Dispatch recalled with id %s for customer %s",
        UUID.randomUUID().toString(),
        customerId));
  }

  private ResponseEntity<String> response(String customerId, String extra) {
    return ResponseEntity.ok(String.format("{\n"
            + "  \"body\": \"Goods dispatched with id %s for customer %s\"\n"
            + extra
            + "}",
        UUID.randomUUID().toString(),
        customerId));
  }

  private boolean isStockShort() {
    return stock <= FETCH_THRESHOLD;
  }
}
