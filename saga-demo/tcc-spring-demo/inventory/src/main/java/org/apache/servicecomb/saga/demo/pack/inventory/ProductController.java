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
package org.apache.servicecomb.saga.demo.pack.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ProductController {
  @Autowired
  private InventoryService inventoryService;

  private final AtomicInteger id = new AtomicInteger(0);

  @PostMapping("/order/{userName}/{productName}/{units}")
  @ResponseBody
  public ProductOrder updateInventory(@PathVariable String userName,
      @PathVariable String productName, @PathVariable Integer units) {
    ProductOrder order = new ProductOrder();
    order.setId(id.incrementAndGet());
    order.setUserName(userName);
    order.setProductName(productName);
    order.setUnits(units);
    inventoryService.reserve(order);
    return order;
  }

  @GetMapping("/orderings")
  @ResponseBody
  List<ProductOrder> getAll() {
    return new ArrayList<>(inventoryService.getAllOrders());
  }

  @DeleteMapping("/orderings")
  @ResponseBody
  String clear() {
    inventoryService.clearAllOrders();
    id.set(0);
    return "OK";
  }

}

