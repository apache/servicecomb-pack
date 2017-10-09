package io.servicecomb.saga.core;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static io.servicecomb.saga.core.Operation.TYPE_REST;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class ForwardRecoveryTest {

  private final SagaTask sagaTask = mock(SagaTask.class);

  private final SagaRequest sagaRequest = mock(SagaRequest.class);

  private final ForwardRecovery forwardRecovery = new ForwardRecovery();

  private final SagaRequest sagaRequest2 = new SagaRequestImpl(
      "request-aaa",
      "aaa",
      TYPE_REST,
      new TransactionImpl("/rest/as", "post", emptyMap()),
      new CompensationImpl("/rest/as", "delete", emptyMap()),
      null,
      null,
      1
  );

  @Test
  public void blowsUpWhenTaskIsNotCommitted() throws Exception {
    doThrow(SagaStartFailedException.class).when(sagaTask).commit(sagaRequest);

    try {
      forwardRecovery.apply(sagaTask, sagaRequest);
      expectFailing(SagaStartFailedException.class);
    } catch (SagaStartFailedException ignored) {
    }
  }

  @Test
  public void blowsUpWhenTaskIsNotCommittedWithFailRetryDelaySeconds() throws Exception {
    doThrow(Exception.class).when(sagaTask).commit(sagaRequest2);

    final int[] retryCount = {0};

    Thread t = new Thread(() -> retryCount[0] = forwardRecovery.apply(sagaTask, sagaRequest2));
    t.start();
    Thread.sleep(2000);
    t.interrupt();
    Thread.sleep(200);
    assertThat(retryCount[0], equalTo(1));
  }
}