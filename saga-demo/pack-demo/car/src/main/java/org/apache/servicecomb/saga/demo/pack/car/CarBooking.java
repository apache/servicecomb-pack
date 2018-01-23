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

public class CarBooking {
  private Integer id;
  private String name;
  private Integer cars;
  private boolean confirm;
  private boolean cancel;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getCars() {
    return cars;
  }

  public void setCars(Integer cars) {
    this.cars = cars;
  }

  public boolean isConfirm() {
    return confirm;
  }

  public void setConfirm(boolean confirm) {
    this.confirm = confirm;
  }

  public boolean isCancel() {
    return cancel;
  }

  public void setCancel(boolean cancel) {
    this.cancel = cancel;
  }
}
