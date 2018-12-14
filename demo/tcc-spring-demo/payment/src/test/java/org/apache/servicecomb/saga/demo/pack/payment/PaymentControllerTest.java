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

package org.apache.servicecomb.saga.demo.pack.payment;

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
@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {
  private final Payment somePayment = somePayment();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PaymentService paymentService;

  @Before
  public void setUp() {
    when(paymentService.getAllTransactions()).thenReturn(singletonList(somePayment));
  }

  @Test
  public void retrievesPaymentsFromRepo() throws Exception {
    mockMvc.perform(get("/transactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$.[0].userName", is(somePayment.getUserName())))
        .andExpect(jsonPath("$.[0].balance", is(somePayment.getBalance())))
        .andExpect(jsonPath("$.[0].confirmed", is(somePayment.isConfirmed())))
        .andExpect(jsonPath("$.[0].cancelled", is(somePayment.isCancelled())))
        .andExpect(jsonPath("$.[0].amount", is(somePayment.getAmount())));
  }

  @Test
  public void verifyDeletingOrders() throws Exception {
    mockMvc.perform(delete("/transactions"))
        .andExpect(status().isOk());
  }

  private Payment somePayment() {
    Payment payment = new Payment();
    payment.setId(1);
    payment.setUserName("UserName1");
    payment.setAmount(100);
    payment.setBalance(20);
    payment.setCancelled(true);
    payment.setConfirmed(false);
    return payment;
  }

}
