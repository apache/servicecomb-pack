package io.servicecomb.saga.core;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class ForwardRecoveryTest {

  private final SagaTask sagaTask = mock(SagaTask.class);

  private final Transaction transaction = mock(Transaction.class);
  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaResponse parentResponse = mock(SagaResponse.class);

  private final ForwardRecovery forwardRecovery = new ForwardRecovery();

  private final String serviceName = "aaa";

  @Before
  public void setUp() throws Exception {
    when(sagaRequest.serviceName()).thenReturn(serviceName);
    when(sagaRequest.transaction()).thenReturn(transaction);
    when(sagaRequest.failRetryDelayMilliseconds()).thenReturn(300);
  }

  @Test
  public void blowsUpWhenTaskIsNotCommittedWithFailRetryDelaySeconds() throws Exception {
    doThrow(Exception.class).when(transaction).send(serviceName, parentResponse);

    Thread t = new Thread(() -> forwardRecovery.apply(sagaTask, sagaRequest, parentResponse));
    t.start();
    Thread.sleep(400);
    t.interrupt();

    verify(transaction, times(2)).send(serviceName, parentResponse);
  }
}