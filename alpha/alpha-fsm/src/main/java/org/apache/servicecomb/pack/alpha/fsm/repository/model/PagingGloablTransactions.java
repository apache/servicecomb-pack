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

  public List<GloablTransaction> getGloablTransactions() {
    return gloablTransactions;
  }

  private List<GloablTransaction> gloablTransactions;

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private long total;
    private int page;
    private int size;
    private long elapsed;
    private List<GloablTransaction> gloablTransactions;

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

    public Builder gloablTransactions(List<GloablTransaction> gloablTransactions) {
      this.gloablTransactions = gloablTransactions;
      return this;
    }

    public PagingGloablTransactions build() {
      PagingGloablTransactions pagingGloablTransactions = new PagingGloablTransactions();
      pagingGloablTransactions.total = this.total;
      pagingGloablTransactions.gloablTransactions = this.gloablTransactions;
      pagingGloablTransactions.page = this.page;
      pagingGloablTransactions.size = this.size;
      pagingGloablTransactions.elapsed = this.elapsed;
      return pagingGloablTransactions;
    }
  }
}
