package io.servicecomb.saga.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SagaContextImpl implements SagaContext {
  private final Map<String, SagaResponse> completedTransactions;
  private final Map<String, SagaResponse> completedCompensations;
  private final Set<String> abortedTransactions;
  private final Map<String, SagaRequest> hangingTransactions;

  SagaContextImpl() {
    this.completedTransactions = new HashMap<>();
    this.completedCompensations = new HashMap<>();
    this.abortedTransactions = new HashSet<>();
    this.hangingTransactions = new HashMap<>();
  }

  @Override
  public boolean isCompensationStarted() {
    return !abortedTransactions.isEmpty() || !completedCompensations.isEmpty();
  }

  @Override
  public void beginTransaction(SagaRequest request) {
    hangingTransactions.put(request.id(), request);
  }

  @Override
  public void endTransaction(SagaRequest request, SagaResponse response) {
    completedTransactions.put(request.id(), response);
    hangingTransactions.remove(request.id());
  }

  @Override
  public void abortTransaction(SagaRequest request) {
    completedTransactions.remove(request.id());
    abortedTransactions.add(request.id());
    hangingTransactions.remove(request.id());
  }

  @Override
  public void compensateTransaction(SagaRequest request, SagaResponse response) {
    completedCompensations.put(request.id(), response);
  }

  @Override
  public Map<String, SagaResponse> completedTransactions() {
    return completedTransactions;
  }

  @Override
  public Map<String, SagaResponse> completedCompensations() {
    return completedCompensations;
  }

  @Override
  public Map<String, SagaRequest> hangingTransactions() {
    return hangingTransactions;
  }
}