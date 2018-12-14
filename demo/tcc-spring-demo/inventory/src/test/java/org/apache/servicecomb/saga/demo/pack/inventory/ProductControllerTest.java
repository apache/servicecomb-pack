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

import static java.util.Collections.singletonList;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;


@RunWith(SpringRunner.class)
@WebMvcTest(ProductController.class)
public class ProductControllerTest {
  private final ProductOrder someOrder = someOrder();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InventoryService inventoryService;

  @Before
  public void setUp() {
    when(inventoryService.getAllOrders()).thenReturn(singletonList(someOrder));
  }

  @Test
  public void retrievesOrdersFromRepo() throws Exception {
    mockMvc.perform(get("/orderings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$.[0].userName", is(someOrder.getUserName())))
        .andExpect(jsonPath("$.[0].productName", is(someOrder.getProductName())))
        .andExpect(jsonPath("$.[0].confirmed", is(someOrder.isConfirmed())))
        .andExpect(jsonPath("$.[0].cancelled", is(someOrder.isCancelled())))
        .andExpect(jsonPath("$.[0].units", is(someOrder.getUnits())));
  }

  @Test
  public void verifyDeletingOrders() throws Exception {
    mockMvc.perform(delete("/orderings"))
        .andExpect(status().isOk());
  }

  private ProductOrder someOrder() {
    ProductOrder order = new ProductOrder();
    order.setId(1);
    order.setProductName("ProductName1");
    order.setUserName("UserName");
    order.setUnits(100);
    order.setCancelled(false);
    order.setConfirmed(true);
    return order;
  }

}
