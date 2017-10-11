package io.servicecomb.saga.core;

public class TransportFailedException extends RuntimeException {
  public TransportFailedException(String cause) {
    super(cause);
  }

  public TransportFailedException(String cause, Throwable e) {
    super(cause, e);
  }
}
