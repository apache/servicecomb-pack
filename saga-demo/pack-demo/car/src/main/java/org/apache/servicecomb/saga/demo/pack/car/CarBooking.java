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

package org.apache.servicecomb.saga.demo.pack.car;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
class CarBooking {
  @JsonIgnore
  private Integer id;
  private String name;
  private Integer amount;
  private boolean confirmed;
  private boolean cancelled;

  Integer getId() {
    return id;
  }

  void setId(Integer id) {
    this.id = id;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  Integer getAmount() {
    return amount;
  }

  void setAmount(Integer amount) {
    this.amount = amount;
  }

  boolean isConfirmed() {
    return confirmed;
  }

  void confirm() {
    this.confirmed = true;
    this.cancelled = false;
  }

  boolean isCancelled() {
    return cancelled;
  }

  void cancel() {
    this.confirmed = false;
    this.cancelled = true;
  }
}
