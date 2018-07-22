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

package org.apache.servicecomb.saga.demo.payment;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

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
@RestSchema(schemaId = "payment-endpoint")
public class PaymentController {
  private int balance = 1000;

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "authenticated user"),
      @ApiResponse(code = 403, response = String.class, message = "unauthenticated user"),
      @ApiResponse(code = 400, response = String.class, message = "insufficient account balance")
  })
  @RequestMapping(value = "payments", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> pay(@RequestAttribute String customerId) {
    if ("anonymous".equals(customerId)) {
      throw new InvocationException(FORBIDDEN, "No such customer with id " + customerId);
    }

    if (balance < 800) {
      throw new InvocationException(BAD_REQUEST, "Not enough balance in account of customer " + customerId);
    }

    balance -= 800;
    return ResponseEntity.ok(String.format("{\n"
            + "  \"body\": \"Payment made for customer %s and remaining balance is %d\"\n"
            + "}",
        customerId,
        balance));
  }

  @ApiResponses({
      @ApiResponse(code = 200, response = String.class, message = "authenticated user"),
      @ApiResponse(code = 403, response = String.class, message = "unauthenticated user")
  })
  @RequestMapping(value = "payments", method = PUT, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> refund(@RequestAttribute String customerId) {
    if ("anonymous".equals(customerId)) {
      throw new InvocationException(FORBIDDEN, "No such customer with id " + customerId);
    }

    balance += 800;
    return ResponseEntity.ok(String.format("Payment refunded for customer %s and remaining balance is %d",
        customerId,
        balance));
  }
}
