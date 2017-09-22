package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class ForwardRecoveryTest {

  private final SagaTask sagaTask = mock(SagaTask.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);

  private final ForwardRecovery forwardRecovery = new ForwardRecovery();

  @Test
  public void apply() throws Exception {
    doThrow(SagaStartFailedException.class).when(sagaTask).commit(sagaRequest);

    try {
      forwardRecovery.apply(sagaTask, sagaRequest);
      expectFailing(SagaStartFailedException.class);
    } catch (SagaStartFailedException ignored) {
    }
  }

}