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

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;


import org.hamcrest.core.IsNull;

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
public class PaymentServiceTest {
  @Autowired
  private PaymentService paymentService;

  @Autowired
  private AccountDao accountDao;

  private Payment payment = new Payment();

  @Before
  public void setup() {
    accountDao.save(new Account("UserA", 100, 60));
    accountDao.save(new Account("UserB", 10, 10));
    accountDao.save(new Account("UserC", 1, 1));

  }

  @After
  public void cleanUp() {
    accountDao.deleteAll();
    paymentService.clearAllTransactions();
  }
  
  @Test
  public void testPaymentServiceWithConfirmation() {
    payment.setId(1);
    payment.setUserName("UserA");
    payment.setAmount(10);
    paymentService.pay(payment);
    assertThat(paymentService.getAccount(payment).getCredit(), is(50));
    assertThat(paymentService.getAccount(payment).getBalance(), is(100));
    assertThat(paymentService.getAllTransactions().size(), is(1));

    paymentService.confirm(payment);
    assertThat(payment.isCancelled(), is(false));
    assertThat(payment.isConfirmed(), is(true));
    assertThat(paymentService.getAccount(payment).getCredit(), is(50));
    assertThat(paymentService.getAccount(payment).getBalance(), is(90));
    assertThat(payment.getBalance(), is(90));
  }

  @Test
  public void testPaymentServicePaymentJustAsCredit() {
    payment.setId(1);
    payment.setUserName("UserB");
    payment.setAmount(10);
    paymentService.pay(payment);
    assertThat(payment.getBalance(), IsNull.nullValue());
    assertThat(paymentService.getAccount(payment).getCredit(), is(0));
    assertThat(paymentService.getAccount(payment).getBalance(), is(10));
    assertThat(paymentService.getAllTransactions().size(), is(1));

    paymentService.confirm(payment);
    assertThat(payment.isCancelled(), is(false));
    assertThat(payment.isConfirmed(), is(true));
    assertThat(paymentService.getAccount(payment).getCredit(), is(0));
    assertThat(paymentService.getAccount(payment).getBalance(), is(0));
    assertThat(payment.getBalance(), is(0));
  }


  @Test
  public void getNoExitUserFromPaymentService() {
    payment.setId(1);
    payment.setAmount(10);
    payment.setUserName("None");
    try {
      paymentService.pay(payment);
      fail("Expect exception here");
    } catch (Exception ex) {
      assertThat(ex.getMessage(), is("Cannot find the account!"));
    }
    assertThat(paymentService.getAllTransactions().size(), is(0));
  }

  @Test
  public void userAccountWithoutFundFromPaymentService() {
    payment.setAmount(10);
    payment.setUserName("UserC");
    try {
      paymentService.pay(payment);
      fail("Expect exception here");
    } catch (Exception ex) {
      assertThat(ex.getMessage(), is("Insufficient funds!"));
    }
    assertThat(paymentService.getAllTransactions().size(), is(0));
  }

  @Test
  public void testInventoryServiceWithCancel() {
    payment.setUserName("UserA");
    payment.setId(1);
    payment.setAmount(10);
    paymentService.pay(payment);
    assertThat(paymentService.getAccount(payment).getCredit(), is(50));
    assertThat(paymentService.getAllTransactions().size(), is(1));

    paymentService.cancel(payment);
    assertThat(payment.isCancelled(), is(true));
    assertThat(payment.isConfirmed(), is(false));
    assertThat(paymentService.getAllTransactions().size(), is(1));
    assertThat(paymentService.getAccount(payment).getCredit(), is(60));
    assertThat(paymentService.getAccount(payment).getBalance(), is(100));
  }

}
