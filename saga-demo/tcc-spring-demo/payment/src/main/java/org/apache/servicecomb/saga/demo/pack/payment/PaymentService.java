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

import java.math.BigDecimal;
import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private AccountDao accountDao;

  @Transactional
  @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
  public BigDecimal execute(String username, BigDecimal amount) {
    Account account = accountDao.findByUsername(username);

    if (account.getBalance()
        .subtract(account.getReservedAmount())
        .compareTo(amount) > 0) {

      account.setReservedAmount(account.getReservedAmount().add(amount));
      accountDao.save(account);
      return amount;
    } else {
      throw new AccountException("Insufficient funds");
    }
  }

  @Transactional
  public void confirm(String username, BigDecimal amount) {
    Account account = accountDao.findByUsername(username);
    account.setReservedAmount(account.getReservedAmount().subtract(amount));
    account.setBalance(account.getBalance().subtract(amount));
    accountDao.save(account);
  }

  @Transactional
  public void cancel(String username, BigDecimal amount) {
    Account account = accountDao.findByUsername(username);
    account.setReservedAmount(account.getReservedAmount().subtract(amount));
    accountDao.save(account);
  }

  @Autowired
  public void setAccountDao(AccountDao accountDao) {
    this.accountDao = accountDao;
  }
}

class AccountException extends RuntimeException {

  public AccountException() {
  }

  public AccountException(String message) {
    super(message);
  }

  public AccountException(String message, Throwable cause) {
    super(message, cause);
  }

  public AccountException(Throwable cause) {
    super(cause);
  }

  public AccountException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}