package io.servicecomb.saga.core;

public class SagaStartFailedException extends RuntimeException {

  public SagaStartFailedException(String cause, Throwable e) {
    super(cause, e);
  }
}
