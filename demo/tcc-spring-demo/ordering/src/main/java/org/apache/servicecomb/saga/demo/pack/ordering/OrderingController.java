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
package org.apache.servicecomb.saga.demo.pack.ordering;

import org.apache.servicecomb.saga.omega.context.annotations.TccStart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/ordering")
public class OrderingController {
  @Value("${inventory.service.address:http://inventory.servicecomb.io:8080}")
  private String inventoryServiceUrl;

  @Value("${payment.service.address:http://payment.servicecomb.io:8080}")
  private String paymentServiceUrl;

  @Autowired
  private RestTemplate restTemplate;

  @TccStart
  @PostMapping("/order/{userName}/{productName}/{productUnit}/{unitPrice}")
  @ResponseBody
  public String ordering(
      @PathVariable String userName,
      @PathVariable String productName, @PathVariable Integer productUnit, @PathVariable Integer unitPrice) {

    restTemplate.postForEntity(
        inventoryServiceUrl + "/order/{userName}/{productName}/{productUnit}",
        null, String.class, userName, productName, productUnit);

    int amount = productUnit * unitPrice;
    
    restTemplate.postForEntity(paymentServiceUrl + "/pay/{userName}/{amount}",
        null, String.class, userName, amount);

    return userName + " ordering " + productName + " with " + productUnit + " OK";
  }

}
