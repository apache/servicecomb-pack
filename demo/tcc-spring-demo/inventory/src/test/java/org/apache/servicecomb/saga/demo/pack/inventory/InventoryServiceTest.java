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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
// Using test profile to avoid the Omega connection
@ActiveProfiles("test")
@SpringBootTest(classes = {TestApplication.class})
public class InventoryServiceTest {
  @Autowired
  private InventoryService inventoryService;

  @Autowired
  private ProductDao productDao;

  private ProductOrder order = new ProductOrder();

  @Before
  public void setup() {
    productDao.save(new Product("ProductA", 100));
    productDao.save(new Product("ProductB", 10));
    productDao.save(new Product("ProductC", 1));

  }

  @After
  public void cleanUp() {
    productDao.deleteAll();
    inventoryService.clearAllOrders();
  }
  
  @Test
  public void testInventoryServiceWithConfirmation() {
    order.setId(1);
    order.setUserName("user1");
    order.setUnits(10);
    order.setProductName("ProductA");
    inventoryService.reserve(order);
    assertThat(inventoryService.getInventory("ProductA"), is(90));
    assertThat(inventoryService.getAllOrders().size(), is(1));

    inventoryService.confirm(order);
    assertThat(order.isCancelled(), is(false));
    assertThat(order.isConfirmed(), is(true));
    assertThat(inventoryService.getInventory("ProductA"), is(90));
  }

  @Test
  public void getNoExitProductFromInventoryService() {
    order.setId(1);
    order.setUnits(10);
    order.setProductName("Product");
    try {
      inventoryService.reserve(order);
      fail("Expect exception here");
    } catch (Exception ex) {
      assertThat(ex.getMessage(), is("Product not exists at all!"));
    }
    assertThat(inventoryService.getAllOrders().size(), is(0));
  }

  @Test
  public void getProductOutOfStockFromInventoryService() {
    order.setUnits(10);
    order.setProductName("ProductC");
    try {
      inventoryService.reserve(order);
      fail("Expect exception here");
    } catch (Exception ex) {
      assertThat(ex.getMessage(), is("The Product is out of stock!"));
    }
    assertThat(inventoryService.getAllOrders().size(), is(0));
  }

  @Test
  public void testInventoryServiceWithCancel() {order.setUserName("user1");
    order.setId(1);
    order.setUnits(10);
    order.setProductName("ProductA");
    inventoryService.reserve(order);
    assertThat(inventoryService.getInventory("ProductA"), is(90));
    assertThat(inventoryService.getAllOrders().size(), is(1));

    inventoryService.cancel(order);
    assertThat(order.isCancelled(), is(true));
    assertThat(order.isConfirmed(), is(false));
    assertThat(inventoryService.getAllOrders().size(), is(1));
    assertThat(inventoryService.getInventory("ProductA"), is(100));
  }

}
