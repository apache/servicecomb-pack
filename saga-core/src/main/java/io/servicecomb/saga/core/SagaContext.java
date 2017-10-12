package io.servicecomb.saga.core;

import java.util.Map;

public interface SagaContext {
  boolean isCompensationStarted();

  void beginTransaction(SagaRequest request);

  void endTransaction(SagaRequest request, SagaResponse response);

  void abortTransaction(SagaRequest request);

  void compensateTransaction(SagaRequest request, SagaResponse response);

  Map<String, SagaResponse> completedTransactions();

  Map<String, SagaResponse> completedCompensations();

  Map<String, SagaRequest> hangingTransactions();
}
