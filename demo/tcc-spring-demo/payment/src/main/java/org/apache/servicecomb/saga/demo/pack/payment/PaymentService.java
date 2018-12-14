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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  @Autowired
  private AccountDao accountDao;

  private Map<Integer, Payment> payments = new ConcurrentHashMap<>();

  @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
  @Transactional
  public void pay(Payment payment) {
    Account account = getAccount(payment);
    if (account.getCredit() >= payment.getAmount()) {
      account.setCredit(account.getCredit() - payment.getAmount());
      accountDao.saveAndFlush(account);
      payments.put(payment.getId(), payment);
    } else {
      throw new IllegalArgumentException("Insufficient funds!");
    }
  }

  @Transactional
  Account getAccount(Payment payment) {
    Account account = accountDao.findByUserName(payment.getUserName());
    if (Objects.isNull(account)) {
      throw new IllegalArgumentException("Cannot find the account!");
    }
    return account;
  }

  @Transactional
  public void confirm(Payment payment) {
    Account account = getAccount(payment);
    payment.setConfirmed(true);
    payment.setCancelled(false);
    account.setBalance(account.getBalance() - payment.getAmount());
    payment.setBalance(account.getBalance());
    accountDao.saveAndFlush(account);


  }

  @Transactional
  public void cancel(Payment payment) {
    Account account = getAccount(payment);
    account.setCredit(account.getCredit() + payment.getAmount());
    accountDao.saveAndFlush(account);
    payment.setBalance(account.getBalance());
    payment.setConfirmed(false);
    payment.setCancelled(true);

  }

  public Collection<Payment> getAllTransactions() {
    return payments.values();
  }

  public void clearAllTransactions() {
    payments.clear();
  }
}

