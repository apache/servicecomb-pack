package io.servicecomb.saga.core;

public interface Transaction extends Operation {

  Transaction SAGA_START_TRANSACTION = new Transaction() {
  };

  Transaction SAGA_END_TRANSACTION = new Transaction() {
  };
}
