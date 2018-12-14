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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class Payment {
  @JsonIgnore
  private Integer id;
  private String userName;
  private Integer amount;
  private Integer balance;
  private boolean confirmed;
  private boolean cancelled;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public Integer getBalance() {
    return balance;
  }

  public void setBalance(Integer balance) {
    this.balance = balance;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }
}
