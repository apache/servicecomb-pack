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

package org.apache.servicecomb.pack.alpha.ui.vo;

import java.util.HashMap;
import java.util.List;

public class DataTablesRequestDTO {

  private int draw;
  private List<HashMap<String, String>> columns;
  private List<HashMap<String, String>> order;
  private int start;
  private int length;
  private String query;
  private String state;

  public int getDraw() {
    return draw;
  }

  public void setDraw(int draw) {
    this.draw = draw;
  }

  public List<HashMap<String, String>> getColumns() {
    return columns;
  }

  public void setColumns(List<HashMap<String, String>> columns) {
    this.columns = columns;
  }

  public List<HashMap<String, String>> getOrder() {
    return order;
  }

  public void setOrder(List<HashMap<String, String>> order) {
    this.order = order;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
