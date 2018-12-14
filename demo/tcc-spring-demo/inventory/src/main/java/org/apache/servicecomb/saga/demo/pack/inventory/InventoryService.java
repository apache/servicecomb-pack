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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

  private Map<Integer, ProductOrder> orders = new ConcurrentHashMap<>();
  @Autowired
  private ProductDao productDao;

  @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
  @Transactional
  public void reserve(ProductOrder order) {
    Product product = getProduct(order.getProductName());
    if (product.getInStock() >= order.getUnits()) {
      product.setInStock(product.getInStock() - order.getUnits());
      productDao.saveAndFlush(product);
      orders.put(order.getId(), order);
    } else {
      throw new IllegalArgumentException("The Product is out of stock!");
    }
  }

  public void confirm(ProductOrder order) {
    order.setConfirmed(true);
  }

  @Transactional
  public void cancel(ProductOrder order) {
    Product product = productDao.findProduceByName(order.getProductName());
    product.setInStock(product.getInStock() + order.getUnits());
    productDao.saveAndFlush(product);
    order.setCancelled(true);
  }

  @Transactional
  private Product getProduct(String productName) {
    Product product = productDao.findProduceByName(productName);
    if (Objects.isNull(product)) {
      throw new IllegalArgumentException("Product not exists at all!");
    }
    return product;
  }
  
  Integer getInventory(String productName) {
    Product product = getProduct(productName);
    return product.getInStock();
  }

  Collection<ProductOrder> getAllOrders() {
    return orders.values();
  }

  void clearAllOrders() {
    orders.clear();
  }
}
