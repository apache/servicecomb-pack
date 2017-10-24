package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class BackwardRecoveryTest {

  private final SagaTask sagaTask = mock(SagaTask.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaResponse parentResponse = mock(SagaResponse.class);
  private final BackwardRecovery backwardRecovery = new BackwardRecovery();

  @Test
  public void blowsUpWhenTaskIsNotCommitted() {
    doThrow(SagaStartFailedException.class).when(sagaTask).commit(sagaRequest, parentResponse);

    try {
      backwardRecovery.apply(sagaTask, sagaRequest, parentResponse);
      expectFailing(SagaStartFailedException.class);
    } catch (SagaStartFailedException ignored) {
    }
  }
}