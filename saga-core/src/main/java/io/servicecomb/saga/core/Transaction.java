package io.servicecomb.saga.core;

public interface Transaction extends Operation, Sender {

  Transaction SAGA_START_TRANSACTION = new Transaction() {
  };

  Transaction SAGA_END_TRANSACTION = new Transaction() {
  };
}
