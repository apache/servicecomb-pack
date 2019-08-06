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

import java.util.ArrayList;
import java.util.List;

public class DataTablesResponseDTO {
  private int draw;
  private int recordsTotal;
  private int recordsFiltered;
  private List<TransactionRowDTO> data = new ArrayList<>();

  public int getDraw() {
    return draw;
  }

  public int getRecordsTotal() {
    return recordsTotal;
  }

  public int getRecordsFiltered() {
    return recordsFiltered;
  }

  public List<TransactionRowDTO> getData() {
    return data;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private int draw;
    private int recordsTotal;
    private int recordsFiltered;
    private List<TransactionRowDTO> data = new ArrayList<>();

    private Builder() {
    }

    public Builder draw(int draw) {
      this.draw = draw;
      return this;
    }

    public Builder recordsTotal(int recordsTotal) {
      this.recordsTotal = recordsTotal;
      return this;
    }

    public Builder recordsFiltered(int recordsFiltered) {
      this.recordsFiltered = recordsFiltered;
      return this;
    }

    public Builder data(List<TransactionRowDTO> data) {
      this.data = data;
      return this;
    }

    public DataTablesResponseDTO build() {
      DataTablesResponseDTO dataTablesResponseDTO = new DataTablesResponseDTO();
      dataTablesResponseDTO.recordsTotal = this.recordsTotal;
      dataTablesResponseDTO.recordsFiltered = this.recordsFiltered;
      dataTablesResponseDTO.draw = this.draw;
      dataTablesResponseDTO.data = this.data;
      return dataTablesResponseDTO;
    }
  }
}
