package org.apache.servicecomb.saga.core;

public interface TransactionConsumer<T> {
  void accept(T request);
}
