package io.servicecomb.saga.core;

import java.util.List;
import java.util.Optional;

public class CompositeSagaResponse implements SagaResponse {
  private final List<SagaResponse> responses;

  public CompositeSagaResponse(List<SagaResponse> responses) {
    this.responses = responses;
  }

  @Override
  public boolean succeeded() {
    return responses.stream().allMatch(SagaResponse::succeeded);
  }

  @Override
  public String body() {
    Optional<String> reduce = responses.stream()
        .map(SagaResponse::body)
        .reduce((a, b) -> a + ", " + b)
        .map(combined -> "[" + combined + "]");

    return reduce.orElse("{}");
  }
}
