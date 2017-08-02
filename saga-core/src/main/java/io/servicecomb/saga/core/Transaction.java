package io.servicecomb.saga.core;

public interface Transaction extends Operation {

  Transaction SAGA_START_TRANSACTION = () -> {
  };

  Transaction SAGA_END_TRANSACTION = () -> {
  };
}
