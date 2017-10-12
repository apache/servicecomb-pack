package io.servicecomb.saga.core;

public interface PersistentLog {
  void offer(SagaEvent sagaEvent);
}
