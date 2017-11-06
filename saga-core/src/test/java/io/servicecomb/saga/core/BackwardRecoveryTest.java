package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class BackwardRecoveryTest {

  private final String serviceName = "aaa";
  private final Transaction transaction = mock(Transaction.class);
  private final SagaTask sagaTask = mock(SagaTask.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaResponse parentResponse = mock(SagaResponse.class);
  private final BackwardRecovery backwardRecovery = new BackwardRecovery();
  private final RuntimeException exception = new RuntimeException("oops");

  @Before
  public void setUp() throws Exception {
    when(sagaRequest.serviceName()).thenReturn(serviceName);
    when(sagaRequest.transaction()).thenReturn(transaction);
  }

  @Test
  public void blowsUpWhenTaskIsNotCommitted() {
    doThrow(exception).when(transaction).send(serviceName, parentResponse);

    try {
      backwardRecovery.apply(sagaTask, sagaRequest, parentResponse);
      expectFailing(RuntimeException.class);
    } catch (RuntimeException ignored) {
    }

    verify(sagaTask).abort(sagaRequest, exception);
  }
}