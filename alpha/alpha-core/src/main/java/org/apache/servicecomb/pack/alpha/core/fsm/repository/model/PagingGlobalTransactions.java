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

package org.apache.servicecomb.pack.alpha.core.fsm.repository.model;

import java.util.List;

public class PagingGlobalTransactions {
  private long total;
  private int page;
  private int size;
  private long elapsed;

  public long getTotal() {
    return total;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public long getElapsed() {
    return elapsed;
  }

  public List<GlobalTransaction> getGlobalTransactions() {
    return globalTransactions;
  }

  private List<GlobalTransaction> globalTransactions;

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private long total;
    private int page;
    private int size;
    private long elapsed;
    private List<GlobalTransaction> globalTransactions;

    private Builder() {
    }

    public Builder total(long total) {
      this.total = total;
      return this;
    }

    public Builder page(int page) {
      this.page = page;
      return this;
    }

    public Builder size(int size) {
      this.size = size;
      return this;
    }

    public Builder elapsed(long elapsed) {
      this.elapsed = elapsed;
      return this;
    }

    public Builder globalTransactions(List<GlobalTransaction> globalTransactions) {
      this.globalTransactions = globalTransactions;
      return this;
    }

    public PagingGlobalTransactions build() {
      PagingGlobalTransactions pagingGlobalTransactions = new PagingGlobalTransactions();
      pagingGlobalTransactions.total = this.total;
      pagingGlobalTransactions.globalTransactions = this.globalTransactions;
      pagingGlobalTransactions.page = this.page;
      pagingGlobalTransactions.size = this.size;
      pagingGlobalTransactions.elapsed = this.elapsed;
      return pagingGlobalTransactions;
    }
  }
}
