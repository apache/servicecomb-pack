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

import java.util.Objects;
import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

  private ProductDao productDao;

  /**
   * Find the product with specific product Id.
   *
   * @param productId Product ID
   * @param requiredCount Required product count
   * @return return the reserved count, 0 if nothing is available. returns negative value if
   * insufficient.
   */
  @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
  public Integer reserve(Long productId, int requiredCount) {
    Product product = productDao.findOne(productId);
    if (Objects.isNull(product)) {
      throw new IllegalArgumentException("Product not exists at all");
    }

    // if it is sufficient
    if (product.getInStock() > requiredCount) {
      product.setInStock(product.getInStock() - requiredCount);
      productDao.save(product);
      return requiredCount;
    } else {
      return product.getInStock() - requiredCount;
    }
  }

  public void confirm(Long productId, int requiredCount) {
    // empty body
  }

  public void cancel(Long productId, int requiredCount) {
    Product product = productDao.findOne(productId);
    product.setInStock(product.getInStock() + requiredCount);
    productDao.save(product);
  }

  @Autowired
  public void setProductDao(ProductDao productDao) {
    this.productDao = productDao;
  }
}
