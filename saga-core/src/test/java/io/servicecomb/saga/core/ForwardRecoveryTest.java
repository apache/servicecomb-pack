package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static io.servicecomb.saga.core.Operation.TYPE_REST;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class ForwardRecoveryTest {

  private final SagaTask sagaTask = mock(SagaTask.class);

  private final SagaRequest sagaRequest = mock(SagaRequest.class);
  private final SagaResponse parentResponse = mock(SagaResponse.class);

  private final ForwardRecovery forwardRecovery = new ForwardRecovery();

  private final SagaRequest sagaRequest2 = new SagaRequestImpl(
      "request-aaa",
      "aaa",
      TYPE_REST,
      new TransactionImpl("/rest/as", "post", emptyMap()),
      new CompensationImpl("/rest/as", "delete", emptyMap()),
      null,
      null,
      300
  );

  @Test
  public void blowsUpWhenTaskIsNotCommitted() throws Exception {
    doThrow(SagaStartFailedException.class).when(sagaTask).commit(sagaRequest, parentResponse);

    try {
      forwardRecovery.apply(sagaTask, sagaRequest, parentResponse);
      expectFailing(SagaStartFailedException.class);
    } catch (SagaStartFailedException ignored) {
    }
  }

  @Test
  public void blowsUpWhenTaskIsNotCommittedWithFailRetryDelaySeconds() throws Exception {
    doThrow(Exception.class).when(sagaTask).commit(sagaRequest2, parentResponse);

    Thread t = new Thread(() -> forwardRecovery.apply(sagaTask, sagaRequest2, parentResponse));
    t.start();
    Thread.sleep(400);
    t.interrupt();

    verify(sagaTask,times(2)).commit(sagaRequest2, parentResponse);
  }
}