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

package org.apache.servicecomb.saga.demo.conditional.transaction.inventory;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Controller
@RequestMapping("/")
public class InventoryController {
  private static final int FETCH_THRESHOLD = 10;
  private static final String CUSTOMER_ID = "customerId";
  private final ObjectMapper objectMapper = new ObjectMapper();

  private int stock = 11;

  @RequestMapping(value = "inventory", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> dispatch(@RequestParam String response) {
    try {
      ObjectNode jsonNodes = objectMapper.readValue(response, ObjectNode.class);
      if (jsonNodes.has(CUSTOMER_ID)) {
        String customerId = jsonNodes.get(CUSTOMER_ID).textValue();

        stock--;
        if (isStockShort()) {
          // when no sagaChildren is provided, all child sub-transaction of inventory will be run
          return response(customerId, "");
        }

        // select no child sub-transaction to run next, by specifying none in sagaChildren
        return response(customerId,",  \"sagaChildren\": [\"none\"] \n");
      }
      return new ResponseEntity<>("Customer Id is missing", BAD_REQUEST);
    } catch (IOException e) {
      return new ResponseEntity<>(e.getMessage(), INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(value = "inventory", method = PUT, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> recall(@RequestParam String customerId) {
    stock++;
    return ResponseEntity.ok(String.format("Dispatch recalled with id %s for customer %s",
        UUID.randomUUID().toString(),
        customerId));
  }

  private ResponseEntity<String> response(String customerId, String optionalSagaChildren) {
    return ResponseEntity.ok(String.format("{\n"
            + "  \"body\": \"Goods dispatched with id %s for customer %s\"\n"
            + optionalSagaChildren
            + "}",
        UUID.randomUUID().toString(),
        customerId));
  }

  private boolean isStockShort() {
    return stock < FETCH_THRESHOLD;
  }
}
