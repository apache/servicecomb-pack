package org.apache.servicecomb.pack.alpha.fsm.repository.model;

import java.util.List;

public class PagingGloablTransactions {
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

    public Builder gloablTransactions(List<GlobalTransaction> globalTransactions) {
      this.globalTransactions = globalTransactions;
      return this;
    }

    public PagingGloablTransactions build() {
      PagingGloablTransactions pagingGloablTransactions = new PagingGloablTransactions();
      pagingGloablTransactions.total = this.total;
      pagingGloablTransactions.globalTransactions = this.globalTransactions;
      pagingGloablTransactions.page = this.page;
      pagingGloablTransactions.size = this.size;
      pagingGloablTransactions.elapsed = this.elapsed;
      return pagingGloablTransactions;
    }
  }
}
