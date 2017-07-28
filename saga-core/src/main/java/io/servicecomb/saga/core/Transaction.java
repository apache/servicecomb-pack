package io.servicecomb.saga.core;

public interface Transaction extends Operation {

  Transaction NO_OP_TRANSACTION = () -> {
  };
}
